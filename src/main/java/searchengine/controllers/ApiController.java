package searchengine.controllers;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.MessageResponse;
import searchengine.dto.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;
import searchengine.services.Storage;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<JSONObject> startIndexing() {
        JSONObject response = new JSONObject();
        if (Storage.getIsIndexing()) {
            return new ResponseEntity<>(responseError("Индексация уже запущена"), HttpStatus.I_AM_A_TEAPOT);
        }
        Storage.setIsIndexing(true);
        statisticsService.startIndexing();
        response.put("result", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<JSONObject> stopIndexing() {
        JSONObject response = new JSONObject();
        if (!Storage.getIsIndexing()) {
            return new ResponseEntity<>(responseError("Индексация не запущена"), HttpStatus.I_AM_A_TEAPOT);
        }
        response.put("result", true);
        Storage.setIsIndexing(false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(String url) {
        MessageResponse response =  statisticsService.indexPage(url);
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(responseError(response.getMessage()), HttpStatus.I_AM_A_TEAPOT);
    }

    @GetMapping("/search")
    public ResponseEntity search(String query, int offset, int limit, String site) {
        SearchResponse response =  statisticsService.search(query, offset, limit, site);
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return new ResponseEntity<>(responseError(response.getMessage()), HttpStatus.I_AM_A_TEAPOT);
        }
    }

    private static JSONObject responseError(String message) {
        JSONObject response = new JSONObject();
        response.put("result", false);
        response.put("error", message);
        return response;
    }

}