package searchengine.services;

import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import searchengine.dto.MessageResponse;

public interface IndexService {
    ResponseEntity<JSONObject> stopIndexing();

    MessageResponse indexPage(String url);

}
