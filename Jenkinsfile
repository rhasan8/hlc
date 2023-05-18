@Library("com.optum.jenkins.pipeline.library@master") _
pipeline {
	agent {
		label 'docker-kitchensink-slave'
	}
	environment {
		TWISTLOCK_CREDS = 'rapid_ai'
		SONAR_CREDS = 'optum-sonar-key'
		DOCKER_CREDS = 'docker_creds'
		DOCKER = '/tools/docker/docker-18.06.1/docker'
		DOCKER_HOST_FOR_TAG = 'docker.repo1.uhc.com'
		DOCKER_ORG = 'image_processing_platform'
		DOCKER_REPO = 'k8-pod-healthcheck'
		DOCKER_COMPOSE = '/tools/docker_compose/docker-compose-1.21.2/docker-compose'
	}
	stages {
		stage('Get Git Info') {
			steps {
				glGitGetInfo()
			}
		}
		stage('Update submodules') {
			steps {
				sh 'git submodule update --init --recursive'
			}
		}
		stage('Docker Build') {
			steps {
			script {
                TAG = VersionNumber(versionNumberString: 'v${BUILD_NUMBER}')}
 			   	sh "$env.DOCKER build -t $env.DOCKER_HOST_FOR_TAG/$env.DOCKER_ORG/$env.DOCKER_REPO:$TAG ."
            }
		}
		stage ('Run Sonar Scan'){
				steps{
							glSonarMavenScan gitUserCredentialsId:"$env.SONAR_CREDS",
							sources:"${WORKSPACE}",
							sonarTool:"sonar",
							sonarProjectVersion:"${TAG}.${BUILD_NUMBER}",
							sonarServer:"sonar.optum",
							sonarExclusions:"**/Scheduler.java,**/SendEmail.java,**/StorageEmail.java",
							sonarTests:"${WORKSPACE}/src/test/java/**",
							additionalProps:
							['sonar.sourceEncoding':'UTF-8',
							 'sonar.java.binaries':'.',
							 'sonar.sources':'./src/main/java'
							]
							echo "Sonar Complete"
			 }
		}
		stage('Docker Push') {
			steps {
				script {
						withCredentials([
							usernamePassword(credentialsId: "$env.DOCKER_CREDS", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')
						]) {
							sh "$env.DOCKER login -u $DOCKER_USER -p $DOCKER_PASSWORD $env.DOCKER_HOST_FOR_TAG"
							sh "$env.DOCKER push $env.DOCKER_HOST_FOR_TAG/$env.DOCKER_ORG/$env.DOCKER_REPO:$TAG"
						}
				}
			}
		}
		stage('Twistlock scan'){
						steps{
								script {
								 sh "docker pull $env.DOCKER_HOST_FOR_TAG/$env.DOCKER_ORG/$env.DOCKER_REPO:$TAG "
											}
									 glTwistlockScan twistlockCredentials: "$env.TWISTLOCK_CREDS",
									 dockerRepository: "$env.DOCKER_HOST_FOR_TAG/$env.DOCKER_ORG/$env.DOCKER_REPO:$TAG",
									 failBuild: false,
									 vulnerabilityThreshold: "critical",
									 complianceThreshold: "critical"
						}
				}
	}
    post {
         success {
           echo 'SUCCESS'
					 emailext body: "Build SUCCESS for $JOB_NAME with build: ${BUILD_URL} ",
					 subject: "Build SUCCESS-$JOB_NAME",
					 to: 'rapid_ai@optim.com'
         }
         failure {
           echo 'FAIL'
           emailext body: "Build FAILED for $JOB_NAME with build: ${BUILD_URL} ",
           subject: "Build FAILED-$JOB_NAME",
           to: 'rapid_ai@optim.com'
         }
    }
}
