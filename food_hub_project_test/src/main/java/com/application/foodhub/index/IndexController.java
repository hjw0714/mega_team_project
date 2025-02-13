package com.application.foodhub.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.application.foodhub.postLike.PostLikeService;

@Controller
public class IndexController {
	
	@Autowired
	private PostLikeService postLikeService;

	@GetMapping
	public String index() {
		return "redirect:/foodhub";
	}

	@GetMapping("/foodhub")
	public String foodhub(Model model) {
	    
	    List<Map<String, Object>> topLikedPosts = postLikeService.getTopLikedPosts();

	    // ✅ 리스트가 null이거나 비어있으면 빈 리스트로 설정
	    if (topLikedPosts == null || topLikedPosts.isEmpty()) {
	        topLikedPosts = new ArrayList<>();
	    }

	    model.addAttribute("topLikedPosts", topLikedPosts);
	    return "foodhub/index/index";
	}

}
