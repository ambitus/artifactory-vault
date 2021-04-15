/************************************************************************************
 * Download an Artifact from Artifactory.  
 *
 * Ensure that you set the following Environment variables prior to useing this script:
 * ARTIFACTORY_URL
 * ARTIFACTORY_TOKEN
 * ARTIFACTORY_REPO
 * ARTIFACTORY_DOWNLOAD_TARGET
 * ARTIFACTORY_REMOTE_ARTIFACT
 ************************************************************************************/

//Get Artifactory info from the environment
def env = System.getenv()
def url = env['ARTIFACTORY_URL']
def apiKey = env['ARTIFACTORY_TOKEN']
def repo = env['ARTIFACTORY_REPO']
def tarFile = new File(env['ARTIFACTORY_DOWNLOAD_TARGET'])
def remoteArtifact = env['ARTIFACTORY_REMOTE_ARTIFACT']

//Call the ArtifactoryHelpers download method to download the artifact
File artifactoryHelpersFile = new File('./ArtifactoryHelpers.groovy')
Class artifactoryHelpersClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(artifactoryHelpersFile)
GroovyObject artifactoryHelpers = (GroovyObject) artifactoryHelpersClass.newInstance()
artifactoryHelpers.download(url, repo, apiKey, remoteArtifact, tarFile)