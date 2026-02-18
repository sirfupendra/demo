package com.example.demo.controllers;

import com.example.demo.Entity.Person;
import com.example.demo.Service.PersonService;
import com.example.demo.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/persons")
public class PersonController {

    @Autowired
    private PersonRepository personRepository;

   @Autowired
   private PersonService personService;
    @GetMapping
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    @GetMapping("/{name}")
    public Person getPersonByName(@PathVariable String name) {
        return personRepository.findByName(name);
    }

    @PostMapping
    public ResponseEntity<Person> createPerson(@RequestBody Person req) {
        Person p= personService.createperson(req.getName(), req.getAge());
        return new  ResponseEntity<>(p,HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deletePerson(@PathVariable String id){
        personRepository.deleteById(id);
        return new ResponseEntity<>("deleted",HttpStatus.NO_CONTENT);
    }
}
