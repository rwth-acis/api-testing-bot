package i5.las2peer.services.apiTestingBot.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.openapitools.openapidiff.core.model.ChangedMetadata;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.Endpoint;
import org.openapitools.openapidiff.core.output.MarkdownRender;

import java.util.List;

/**
 * Extension of MarkdownRender that transforms the OpenAPI difference description from Markdown format into a format
 * that can be displayed within RocketChat.
 */
public class RocketChatRender extends MarkdownRender {

    private static final String CODE = "`";

    @Override
    public String render(ChangedOpenApi diff) {
        super.render(diff);
        String text = "";

        if(!diff.getNewEndpoints().isEmpty())
            text += listEndpoints("What's New", diff.getNewEndpoints(), diff.getNewSpecOpenApi()) + "\n";
        if(!diff.getMissingEndpoints().isEmpty())
            text += listEndpoints("What's Deleted", diff.getMissingEndpoints(), diff.getOldSpecOpenApi()) + "\n";
        if(!diff.getDeprecatedEndpoints().isEmpty())
            text += listEndpoints("What's Deprecated", diff.getDeprecatedEndpoints(), diff.getNewSpecOpenApi()) + "\n";

        text += listEndpoints(diff.getChangedOperations());
        return text;
    }

    @Override
    protected String sectionTitle(String title) {
        return "*" + title + "*" + '\n';
    }

    @Override
    protected String titleH5(String title) {
        return sectionTitle(title);
    }

    protected String itemEndpoint(Endpoint endpoint, OpenAPI openAPI) {
        String method = endpoint.getMethod().toString();
        String path = endpoint.getPathUrl();
        String summary = endpoint.getSummary();

        String text = "*- *" + CODE + method + CODE + " *" + path + "*";
        if(metadata(summary) != null && !metadata(summary).isEmpty()) text += "\n" + metadata(summary);
        text += "\n";

        // include yaml code from OpenAPI document
        Operation o = openAPI.getPaths().get(path).readOperationsMap().get(PathItem.HttpMethod.valueOf(method));
        // remove some fields
        o.getParameters().forEach(param -> param.setStyle(null));
        o.getParameters().forEach(param -> param.explode(null));
        o.getParameters().forEach(param -> {
            if(param.getDescription().isEmpty()) param.setDescription(null);
        });
        o.getResponses().forEach((code, response) -> {
            if(response.getDescription().isEmpty()) response.setDescription(null);
        });
        text += "```\n" + Yaml.pretty(o) + "```";

        return text;
    }

    @Override
    protected String itemEndpoint(String method, String path, ChangedMetadata summary) {
        String text = "*- " + CODE + method + CODE + " " + path + "*";

        // append summary (if exists)
        String metadata = metadata("summary", summary);
        if(metadata != null && !metadata.isEmpty()) text += "\n" + metadata;

        return text + "\n";
    }

    protected String listEndpoints(String title, List<Endpoint> endpoints, OpenAPI openAPI) {
        if (null == endpoints || endpoints.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(sectionTitle(title));
        for(Endpoint endpoint : endpoints) {
            sb.append(itemEndpoint(endpoint, openAPI));
        }
        return sb.toString();
    }
}
