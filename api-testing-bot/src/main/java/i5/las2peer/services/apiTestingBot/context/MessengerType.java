package i5.las2peer.services.apiTestingBot.context;

public enum MessengerType {
    ROCKET_CHAT,
    GITHUB_ISSUES,
    GITHUB_PR;

    public static MessengerType fromString(String messengerType) {
        switch(messengerType) {
            case "Rocket.Chat":
                return ROCKET_CHAT;
            case "GitHub Issues":
                return GITHUB_ISSUES;
            case "GitHub Pull Requests":
                return GITHUB_PR;
            default:
                return ROCKET_CHAT;
        }
    }
}
