package com.wildtrack.service;

import com.wildtrack.client.MovebankClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class MovebankIngestionService {

    @Autowired
    private MovebankClient Movebankclient;

    public String getData(Long id) {
        return Movebankclient.getData(id);
    }
}