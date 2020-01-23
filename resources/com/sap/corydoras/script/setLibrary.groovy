import jenkins.model.*
import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.*

def createIfMissing(String libName, String gitUrl, String defaultVersion) {
    println 'Add Library ' + libName + ', from repository ' + gitUrl + ' with version ' + defaultVersion
    GitSCMSource gitScmSource = new GitSCMSource(null, gitUrl, '', 'origin', '+refs/heads/*:refs/remotes/origin/*', '*', '', true)
    LibraryConfiguration lib = new LibraryConfiguration(libName, new SCMSourceRetriever(gitScmSource))
    lib.defaultVersion = defaultVersion
    lib.implicit = false
    lib.allowVersionOverride = true

    GlobalLibraries globalLibraries = GlobalLibraries.get()
    List libs = new ArrayList(globalLibraries.libraries)

    boolean exists = false
    for (LibraryConfiguration libConfig : libs) {
        if (libConfig.name == libName) {
            exists = true
            break
        }
    }

    if (!exists) {
        libs.add(lib)
        globalLibraries.setLibraries(libs)
        globalLibraries.save()
        println 'Library ' + libName + ' added'
    }
}
