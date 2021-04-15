/************************************************************************************
 * Upload Artifact to Artifactory
 *
 * Ensure that you set the following Environment variables prior to useing this script:
 * ARTIFACTORY_URL
 * ARTIFACTORY_TOKEN
 * ARTIFACTORY_REPO
 * ARTIFACTORY_UPLOAD
 * ARTIFACTORY_UPLOAD_TARGET
 ************************************************************************************/

//Get Artifactory info from the environment
def env = System.getenv()
def url = env['ARTIFACTORY_URL']
def apiKey = env['ARTIFACTORY_TOKEN']
def repo = env['ARTIFACTORY_REPO']
def tarFile = new File(env['ARTIFACTORY_UPLOAD'])
def remotePath = env['ARTIFACTORY_UPLOAD_TARGET']

//Call the ArtifactoryHelpers publish method to upload the Artifacts
File artifactoryHelpersFile = new File('./ArtifactoryHelpers.groovy')
Class artifactoryHelpersClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(artifactoryHelpersFile)
GroovyObject artifactoryHelpers = (GroovyObject) artifactoryHelpersClass.newInstance()
artifactoryHelpers.publish(url, repo, apiKey, remotePath, tarFile)