pipeline {
        agent none
        stages {
          stage("build & SonarQube analysis") {
            agent any
            steps {
              withSonarQubeEnv('sonarqube') {
                withMaven {
                  sh "mvn clean package sonar:sonar -Dsonar.projectKey=matteo6198_CDC_TDD_AYkGHfQxaje6Mmtcnbqh -Dsonar.projectName='CDC_TDD' -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=sqp_6f50e91139e63b5b5ac63bfaaa5106e2a391ac35"
                }
              }
            }
          }
          stage("Quality Gate") {
            steps {
              timeout(time: 1, unit: 'HOURS') {
                waitForQualityGate abortPipeline: true
              }
            }
          }
        }
      }