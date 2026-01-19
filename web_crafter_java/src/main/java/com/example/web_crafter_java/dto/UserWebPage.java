package com.example.web_crafter_java.dto;

import lombok.Data;

@Data
public class UserWebPage {
    private Integer id;
    private Integer webId;
    private String pageName;
    private String layoutData;
    private String styleData;
    private String logicData;
}