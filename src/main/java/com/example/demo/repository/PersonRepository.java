package com.example.demo.repository;

import com.example.demo.Entity.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends MongoRepository<Person, String> {
    // Custom query example
    Person findByName(String name);
}
