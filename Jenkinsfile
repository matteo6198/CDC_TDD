pipeline {
        agent none
        stages {
          stage("build & SonarQube analysis") {
            agent any
            steps {
              withSonarQubeEnv('sonarqube') {
                withMaven {
                  sh 'mvn clean package sonar:sonar'
                  sh" ${SCANNER_HOME}/bin/sonar-scanner -Dsonar.projectKey=matteo6198_CDC_TDD_AYkGHfQxaje6Mmtcnbqh -Dsonar.sources=. "
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