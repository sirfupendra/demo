package com.example.demo.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "posts")
@Data
@AllArgsConstructor
@Builder
public class Post {
    @Id
    int id;
    String content;
    boolean isPolitical;
    String title;
    @DBRef
    private String owner;
}
