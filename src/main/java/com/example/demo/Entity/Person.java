package com.example.demo.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "persons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    private String id;
    private String name;
    private int age;

    @DBRef
    private List<String> postids=new ArrayList<>();







//    // Constructors
//    public Person() {}
//    public Person(String name, int age) {
//        this.name = name;
//        this.age = age;
//    }
//
//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//    public int getAge() { return age; }
//    public void setAge(int age) { this.age = age; }
}
