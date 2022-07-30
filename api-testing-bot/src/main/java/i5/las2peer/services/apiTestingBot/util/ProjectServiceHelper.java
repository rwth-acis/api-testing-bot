package i5.las2peer.services.apiTestingBot.util;

import i5.las2peer.api.Context;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProjectServiceHelper {

    private static final String SERVICE_NAME = "i5.las2peer.services.projectService.ProjectService";

    public static List<JSONObject> getProjectsLinkedToChannel(String channel) {
        List<JSONObject> projectsLinkedToChannel = new ArrayList<>();
        try {
            JSONObject projectsObj = (JSONObject) Context.get().invoke(SERVICE_NAME, "getProjectsRMI", "CAE");

            List<JSONObject> projects = (List<JSONObject>) projectsObj.get("projects");
            List<JSONObject> projectsWithLinkedChat = projects.stream().filter(project -> project.containsKey("chatInfo")).toList();

            for(JSONObject project : projectsWithLinkedChat) {
                JSONObject chatInfo = (JSONObject) project.get("chatInfo");
                String type = (String) chatInfo.get("type");
                if(type.equals("RocketChat")) {
                    String projectChannelId = (String) chatInfo.get("channelId");
                    if(projectChannelId.equals(channel)) {
                        projectsLinkedToChannel.add(project);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching projects from las2peer-project-service.");
        }
        return projectsLinkedToChannel;
    }
}
