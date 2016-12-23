package ske.aurora.maven.plugins.versionnumber;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import ske.aurora.gitversion.GitTools;
import ske.aurora.gitversion.GitVersion;

public class VersionNumberSuggester {

    public static String suggestVersion() throws IOException {
        return suggestVersion(new Options());
    }

    public static String suggestVersion(Options options) throws IOException {

        Repository repository = getGitRepository();

        if (shouldInferReleaseVersion(repository, options)) {
            return getInferredVersion(repository, options);
        }
        return getVersionFromGit(repository, options);
    }

    private static boolean shouldInferReleaseVersion(Repository repository, Options options) throws IOException {

        Optional<String> currentBranchOption = GitTools.getBranchName(repository, true, "BRANCH_NAME");

        String currentBranch = currentBranchOption
            .orElseThrow(() -> new IllegalStateException("Unable to determine name of current branch"));

        return options.branchesToInferReleaseVersionsFor.contains(currentBranch);
    }

    private static String getInferredVersion(Repository repository, Options options) {

        List<String> versions = getAllVersionsFromTags(repository, options.versionPrefix);
        VersionNumber inferredVersion =
            new ReleaseVersionEvaluator(options.versionHint).suggestNextReleaseVersionFrom(versions);
        return inferredVersion.toString();
    }

    private static String getVersionFromGit(Repository repository, Options options) throws IOException {

        GitVersion.Options gitVersionOptions = new GitVersion.Options();
        gitVersionOptions.setVersionPrefix(options.versionPrefix);

        GitVersion.Version versionFromGit = GitVersion.determineVersion(repository, gitVersionOptions);
        return versionFromGit.getVersion();
    }

    private static List<String> getAllVersionsFromTags(Repository repository, String versionPrefix) {

        return repository.getTags().entrySet().stream()
            .filter(e -> e.getKey().startsWith(versionPrefix))
            .map(e -> e.getKey().replaceFirst(versionPrefix, ""))
            .collect(Collectors.toList());
    }

    private static Repository getGitRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File("./", ".git"))
            .readEnvironment()
            .setMustExist(true)
            .build();
    }

    public static class Options {

        List<String> branchesToInferReleaseVersionsFor = Collections.singletonList("master");

        String versionPrefix = "v";

        String versionHint = null;

        public List<String> getBranchesToInferReleaseVersionsFor() {
            return branchesToInferReleaseVersionsFor;
        }

        public void setBranchesToInferReleaseVersionsFor(List<String> branchesToInferReleaseVersionsFor) {
            this.branchesToInferReleaseVersionsFor = branchesToInferReleaseVersionsFor;
        }

        public String getVersionPrefix() {
            return versionPrefix;
        }

        public void setVersionPrefix(String versionPrefix) {
            this.versionPrefix = versionPrefix;
        }

        public String getVersionHint() {
            return versionHint;
        }

        public void setVersionHint(String versionHint) {
            this.versionHint = versionHint;
        }
    }
}
