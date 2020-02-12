package io.codeontap.freemarkerwrapper.files.meta;

import java.util.List;
import java.util.Set;

public abstract class LayerMeta {
    private Set<String> layersNames;
    private String startingPath;
    private List<String> regexList;

    private boolean ignoreDotDirectories;
    private boolean ignoreDotFiles;
    private boolean includeInformation;
    private boolean addStartingWildcard;
    private boolean addEndingWildcard;

    private Integer minDepth;
    private Integer maxDepth;

    public String getStartingPath() {
        return startingPath;
    }

    public void setStartingPath(String startingPath) {
        this.startingPath = startingPath;
    }

    public List<String> getRegexList() {
        return regexList;
    }

    public void setRegexList(List<String> regexList) {
        this.regexList = regexList;
    }

    public boolean isIgnoreDotDirectories() {
        return ignoreDotDirectories;
    }

    public void setIgnoreDotDirectories(boolean ignoreDotDirectories) {
        this.ignoreDotDirectories = ignoreDotDirectories;
    }

    public boolean isIgnoreDotFiles() {
        return ignoreDotFiles;
    }

    public void setIgnoreDotFiles(boolean ignoreDotFiles) {
        this.ignoreDotFiles = ignoreDotFiles;
    }

    public boolean isIncludeInformation() {
        return includeInformation;
    }

    public void setIncludeInformation(boolean includeInformation) {
        this.includeInformation = includeInformation;
    }

    public Set<String> getLayersNames() {
        return layersNames;
    }

    public void setLayersNames(Set<String> layersNames) {
        this.layersNames = layersNames;
    }

    public boolean isAddStartingWildcard() {
        return addStartingWildcard;
    }

    public void setAddStartingWildcard(boolean addStartingWildcard) {
        this.addStartingWildcard = addStartingWildcard;
    }

    public boolean isAddEndingWildcard() {
        return addEndingWildcard;
    }

    public void setAddEndingWildcard(boolean addEndingWildcard) {
        this.addEndingWildcard = addEndingWildcard;
    }

    public Integer getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(int minDepth) {
        this.minDepth = minDepth;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }
}