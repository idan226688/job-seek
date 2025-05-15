package com.jobseek.controller;

import com.jobseek.model.Cv;
import com.jobseek.repository.CvRepository;
import com.jobseek.service.CvParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cv")
public class CvController {

    private final CvRepository cvRepository;
    private final CvParserService cvParserService;

    public CvController(CvRepository cvRepository, CvParserService cvParserService) {
        this.cvRepository = cvRepository;
        this.cvParserService = cvParserService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCv(@RequestParam("file") MultipartFile file, 
                                            @RequestParam("userId") String userId) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded.");
        }

        String text = cvParserService.extractText(file);

        Cv cv = new Cv();
        cv.setId(userId);
        cv.setContent(text);
        cvRepository.save(cv);

        return ResponseEntity.ok("CV uploaded and saved successfully.");
    }

    @GetMapping("/cvs")
        public List<Cv> getAllCvs() {
    return cvRepository.findAll();
    }


    @GetMapping("/{id}")
        public ResponseEntity<Cv> getCvById(@PathVariable String id) {
        return cvRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
}
