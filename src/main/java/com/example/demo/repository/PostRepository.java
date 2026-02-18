package com.example.demo.repository;

import com.example.demo.Entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends MongoRepository<Post,String> {
    Post findByTitle(String title);
}
