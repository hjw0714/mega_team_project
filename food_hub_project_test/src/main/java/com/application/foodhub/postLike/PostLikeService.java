package com.application.foodhub.postLike;

public interface PostLikeService {

	public void togglePostLike(long postId, String userId);
	public int getPostLikeCount(long postId);
	
}
