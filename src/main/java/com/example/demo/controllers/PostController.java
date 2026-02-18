package com.example.demo.controllers;

import com.example.demo.Entity.Post;
import com.example.demo.Service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    PostService postService;

    @GetMapping("/{title}")
    private ResponseEntity<Post> getPostBYtitle(@PathVariable String req){
        Post p=postService.getPost(req);
        return new ResponseEntity<>(p,HttpStatus.OK);
    }
    @PostMapping("/create")
    private ResponseEntity<Post> createPost(@RequestBody Post req){
        Post p=postService.createPost(req.getContent(),req.getTitle(),req.isPolitical(), req.getOwner());
        return new ResponseEntity<>(p,HttpStatus.CREATED);
    }

}
