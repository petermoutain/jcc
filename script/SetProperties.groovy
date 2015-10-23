//
//project.properties.public_key = new URL("http://192.168.12.180:8080/lic/servlet?type=publickey").text
//project.properties.expired_time = new URL("http://192.168.12.180:8080/lic/servlet?type=expiredate").text
//test auto compile

def proc

proc = "git rev-parse HEAD".execute();
proc.waitFor();

if (proc.exitValue() == 0) {
    def git_commit_id = proc.in.text
    project.properties.git_commit_id = git_commit_id
} else {
    println proc.err.text;
}
