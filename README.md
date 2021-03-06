# Coverity Sonar Plugin

The Coverity Sonar Plugin automatically import issues from Coverity Connect into SonarQube.

## Notes

* This project depends on javax.xml.crypto:xmldsig.jar . This artifact is not in maven central, so you may need to add it to your local repository manually. Here's how to do that:

  * Download the xmldsig jar. (It can be found in several places online)
  * Run this command:
  ```
mvn install:install-file -Dfile=xmldsig-1.0.jar -DgroupId=javax.xml.crypto -DartifactId=xmldsig -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
  ```
  Where "-Dfile=xmldsig-1.0.jar" indicates the path to the downloaded jar file.
  
  Note that this is only required to build the project, not to run it.
  
## Changelog

* __1.4.2__
  * Adding "sonar.coverity.sources.directory" property that overrides "sonar.sources" on coverity's plugin.
  * Enhancing mechanism for handling stripped paths.




