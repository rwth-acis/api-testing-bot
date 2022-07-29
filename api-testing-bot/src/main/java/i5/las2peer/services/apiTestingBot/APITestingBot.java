package i5.las2peer.services.apiTestingBot;

import i5.las2peer.api.ManualDeployment;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;

@Api
@ServicePath("/apitestingbot")
@ManualDeployment
public class APITestingBot extends RESTService {

    private String botManagerURL;

    public APITestingBot() {
        setFieldValues();
    }

}
