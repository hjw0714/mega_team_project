package com.application.foodhub.index;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {


	   
	   @GetMapping("/foodhub")
	   public String foodhub() {
	      return "foodhub/index/index";
	   }
}
