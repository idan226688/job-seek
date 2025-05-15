package com.jobseek.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Cv {
    @Id
    private String id;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    public Cv() {}

    public Cv(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
