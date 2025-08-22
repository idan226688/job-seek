package com.jobseek.model;

import jakarta.persistence.*;

@Entity
public class JobDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    public JobDescription() {}

    public JobDescription(Integer id, String content) {
        this.id = id;
        this.content = content;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
