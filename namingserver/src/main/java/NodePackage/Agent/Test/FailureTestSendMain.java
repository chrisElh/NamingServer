package NodePackage.Agent.Test;

public class FailureTestSendMain {
    public static void main(String[] args) {
        int failedPort = 4060; // kies een poortnummer dat je wilt simuleren als failure
        System.out.println("Sending failure report for port " + failedPort);
        FailureTestSender.sendFailure(failedPort);
        System.out.println("Failure report sent.");
    }
}



