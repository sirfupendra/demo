package com.example.demo.Service;

import com.example.demo.Entity.Person;
import com.example.demo.Entity.Post;
import com.example.demo.repository.PersonRepository;
import com.example.demo.repository.PostRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {
    @Autowired
    PostRepository postRepository;

    @Autowired
    PersonRepository personRepository;

    public Post getPost(String title){
        Post p= postRepository.findByTitle(title);
        return p;
    }

    @Transactional
    public Post createPost(String content, String title, boolean isPolitical, String name){
        Person person=personRepository.findByName(name);
        Post p= Post.builder().content(content).title(title).isPolitical(isPolitical).owner(name).build();

        postRepository.save(p);
        person.getPostids().addLast(String.valueOf(p.getId()));
        personRepository.save(person);
        return  p;
    }
}
