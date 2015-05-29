package pl.nort;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GitConfigurationService implements ConfigurationService, Closeable {

  private static final String LOCAL_REPOSITORY_PATH_IN_TEMP = "nort-config-git-config-repository";

  private final Git clonedRepo;
  private final File clonedRepoPath;

  /**
   * Read configuration from the remote GIT repository residing at {@code repositoryURI}. Keeps a local
   * clone of the repository in the system tmp directory.
   *
   * @param repositoryURI URI to the remote git repository
   * @throws GitConfigurationServiceException when unable to clone repository
   */
  public GitConfigurationService(String repositoryURI) {
    this(repositoryURI, System.getProperty("java.io.tmpdir"), LOCAL_REPOSITORY_PATH_IN_TEMP);
  }

  /**
   * Read configuration from the remote GIT repository residing at {@code repositoryURI}. Keeps a local
   * clone of the repository in the {@code localRepositoryPathInTemp} directory under {@code tmpPath} path.
   *
   * @param repositoryURI             URI to the remote git repository
   * @param tmpPath                   path to the tmp directory
   * @param localRepositoryPathInTemp name of the local directory keeping the repository clone
   * @throws GitConfigurationServiceException when unable to clone repository
   */
  public GitConfigurationService(String repositoryURI, String tmpPath, String localRepositoryPathInTemp) {

    try {
      clonedRepoPath = File.createTempFile(localRepositoryPathInTemp, "", new File(tmpPath));
      // This folder can't exist or JGit will throw NPE on clone
      if (!clonedRepoPath.delete()) {
        throw new GitConfigurationServiceException("Unable to remove temp directory for local clone: " + localRepositoryPathInTemp);
      }
    } catch (IOException e) {
      throw new GitConfigurationServiceException("Unable to create local clone directory: " + localRepositoryPathInTemp, e);
    }

    try {
      clonedRepo = Git.cloneRepository()
          .setURI(repositoryURI)
          .setDirectory(clonedRepoPath)
          .call();
    } catch (GitAPIException e) {
      throw new GitConfigurationServiceException("Unable to clone repository: " + repositoryURI, e);
    }
  }

  @Override
  public Properties getConfiguration() {
    Properties properties = new Properties();
    InputStream input = null;

    try {
      input = new FileInputStream(clonedRepoPath + "/application.properties");
      properties.load(input);
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return properties;
  }

  @Override
  public void close() throws IOException {
    clonedRepo.close();
  }
}