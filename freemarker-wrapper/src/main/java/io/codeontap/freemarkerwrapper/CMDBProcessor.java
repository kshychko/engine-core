package io.codeontap.freemarkerwrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.json.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * TODO: add staringPath processing
 */
public class CMDBProcessor {
    public Map<String, JsonObject> getFileTree(String lookupDir, Map<String, String> CMDBs, final List<String> CMDBNamesList, String baseCMDB, String startingPath, List<String> regex,
                                               boolean ignoreDotDirectories, boolean ignoreDotFiles, boolean includeCMDBInformation, boolean useCMDBPrefix) throws RunFreeMarkerException {
        Map<String, JsonObject> output = new HashMap<String, JsonObject>();
        Map<String, Path> files = new TreeMap<>();
        Set<String> CMDBNames = new TreeSet<>();
        CMDBNames.addAll(CMDBNamesList);
        /*
         * When -g value is provided as a single path.
         * The second form identifies a directory whose subtree is scanned for .cmdb files,
         * with the containing directory being treated as a CMDB whose name is that of the containing directory.
         */
        if (StringUtils.isNotEmpty(lookupDir)) {
            FileFinder.Finder cmdbFilefinder = new FileFinder.Finder(".cmdb", false, false);
            try {
                Files.walkFileTree(Paths.get(lookupDir), cmdbFilefinder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (Path cmdbFile : cmdbFilefinder.done()) {
                String cmdbName = cmdbFile.getParent().getFileName().toString();
                String cmdbPath = cmdbFile.getParent().toString();
                String CMDBPrefix = useCMDBPrefix?cmdbFile.getParent().getParent().getFileName().toString().concat("_"):"";
                CMDBs.put(CMDBPrefix.concat(cmdbName), cmdbPath);
            }
        } else {
            for (String CMDBName : CMDBs.keySet()) {
                String CMDBPath = CMDBs.get(CMDBName);
                try {
                    if (!listFilesUsingDirectoryStream(CMDBs.get(CMDBName)).contains(".cmdb")) {
                        throw new RunFreeMarkerException(
                                String.format("Unable to find .cmdb file in path \"%s\" for CMDB \"%s\"", CMDBPath, CMDBName));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RunFreeMarkerException(
                            String.format("Unable to read path \"%s\" for CMDB \"%s\"", CMDBPath, CMDBName));
                }
            }
        }
        /**
         * when -b option is not specified, the default base CMDB is tenant
         * doesn't expect CMDBPrefixes to be used
         * TODO: add a catch code
         */
        if (StringUtils.isEmpty(baseCMDB)) {
            baseCMDB = "tenant";
        }
        if (!CMDBs.containsKey(baseCMDB)) {
            throw new RunFreeMarkerException(String.format("Base CMDB \"%s\" is missing from the detected CMDBs \"%s\"", baseCMDB, CMDBs));
        }

        Map<String, String> cmdbFileSystem = buildCMDBFileSystem(baseCMDB, CMDBs, useCMDBPrefix, CMDBNames);
        //if a layer for base CMDB was not defined consider "/" as a default basePath for it
        if (!cmdbFileSystem.containsKey(baseCMDB)){
            cmdbFileSystem.put(baseCMDB, "/");
        }

        Map<String, String> cmdbFilesMapping = new TreeMap<>();
        Map<String, String> cmdbPhysicalFilesMapping = new TreeMap<>();

        for (String pattern : regex) {
            for (String CMDBName : cmdbFileSystem.keySet()) {
                if (!CMDBs.containsKey(CMDBName)) {
                    throw new RunFreeMarkerException(String.format("CMDB Name \"%s\" was not found in the detected CMDBs \"%s\"", CMDBName, CMDBs));
                }
                Path startingDir = Paths.get(CMDBs.get(CMDBName));
                FileFinder.Finder finder = new FileFinder.Finder(pattern, ignoreDotDirectories, ignoreDotFiles);
                try {
                    Files.walkFileTree(startingDir, finder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (Path file : finder.done()) {
                    String path = file.toString();
                    String cmdbBasePath = cmdbFileSystem.get(CMDBName);
                    String cmdbPath = StringUtils.replaceOnce(path, startingDir.toString(), cmdbBasePath);
                    cmdbFilesMapping.put(cmdbPath, path);
                    cmdbPhysicalFilesMapping.put(path, CMDBName);
                }
            }
        }

        for (String file : cmdbFilesMapping.keySet()) {
            //TODO: fix the double slashes properly
            files.put(StringUtils.replace(file, "//", "/"), Paths.get(cmdbFilesMapping.get(file)));
        }

        for (String key : files.keySet()) {
            Path file = files.get(key);
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("Path", StringUtils.substringBeforeLast(key, file.getFileName().toString()));
            jsonObjectBuilder.add("Filename", file.getFileName().toString());
            jsonObjectBuilder.add("Extension", StringUtils.substringAfterLast(file.getFileName().toString(), "."));
            try (FileInputStream inputStream = new FileInputStream(file.toString())) {
                jsonObjectBuilder.add("Contents", IOUtils.toString(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (includeCMDBInformation) {
                String cmdbName = cmdbPhysicalFilesMapping.get(file.toString());
                String cmdbBasePath = cmdbFileSystem.get(cmdbName);
                jsonObjectBuilder.add("CMDB",
                        Json.createObjectBuilder().add("Name", cmdbName).add("BasePath", cmdbBasePath).add("File", file.toString()).build());
            }
            output.put(key, jsonObjectBuilder.build());
        }
        return output;
    }

    private Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.getFileName()
                            .toString());
                }
            }
        }
        return fileList;
    }

    private Path readJSONFileUsingDirectoryStream(String dir, String fileName) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    if(fileName.equals(path.getFileName().toString())){
                        return path;
                    }
                }
            }
        }
        return null;
    }


    /**
     * Builds a CMDBFileSystem
     * TODO: add support of relative/absolute path
     * at the moment all paths are absolute
     * @param baseCMDB the name of the base CMDB
     * @param CMDBs
     * @param useCMDBPrefix
     * @return
     */
    private Map<String, String> buildCMDBFileSystem(final String baseCMDB, final Map<String, String> CMDBs, boolean useCMDBPrefix, final Set<String> CMDBNames){
        Map<String, String> cmdbFileSystem = new TreeMap<>();
        JsonReader jsonReader = null;
            try {
                Path CMDBPath = readJSONFileUsingDirectoryStream(CMDBs.get(baseCMDB), ".cmdb");
                jsonReader = Json.createReader(new FileReader(CMDBPath.toFile()));
                JsonObject jsonObject = jsonReader.readObject();
                if (jsonObject.containsKey("Layers")) {
                    JsonArray layers = jsonObject.getJsonArray("Layers").asJsonArray();
                    for (int i = 0; i < layers.size(); i++) {
                        JsonObject layer = layers.getJsonObject(i);
                        String layerName = layer.getString("Name");
                        //if -c option was applied, check if a CMDB layer should be included into the CMDB file system
                        if(!CMDBNames.isEmpty() && !CMDBNames.contains(layerName))
                            continue;
                        String basePath = layer.getString("BasePath");
                        String CMDBPrefix = useCMDBPrefix?CMDBPath.getParent().getParent().getFileName().toString().concat("_"):"";
                        cmdbFileSystem.put(CMDBPrefix.concat(layerName), basePath);
                        if(!baseCMDB.equalsIgnoreCase(layerName)) {
                            cmdbFileSystem.putAll(buildCMDBFileSystem(layerName, CMDBs, useCMDBPrefix, CMDBNames));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        return cmdbFileSystem;
    }
}
