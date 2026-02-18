package com.example.demo.Service;

import com.example.demo.Entity.Person;
import com.example.demo.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    @Autowired
   private PersonRepository personRepository;
    public Person createperson(String name, int age){
        Person person=Person.builder().name(name).age(age).build();
        return personRepository.save(person);

    }
}
