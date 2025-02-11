package com.application.foodhub.post;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostDAO postDAO;

	@Override
	public List<Map<String, Object>> getPostList(int pageSize, int offset) {
		return postDAO.getPostList(pageSize, offset);
	}

	@Override
	public long getAllPostCnt() {
		return postDAO.getAllPostCnt();
	}

	@Override
	public long getPostCnt() {
		long count = postDAO.getPostCnt();
		return Math.max(count, 0); // 0 미만이 되지 않도록 보장
	}

	@Override
	public Map<String, Object> getPostDetail(long postId, boolean isIncreaseReadCnt) {

		if (isIncreaseReadCnt) {
			postDAO.updateReadCnt(postId); // 조회수 증가
		}
		return postDAO.getPostDetail(postId);
	}

	@Override
	public Long createPost(PostDTO postDTO) {
		postDAO.createPost(postDTO);
		return postDTO.getPostId();
	}

	@Override
	public List<Map<String, Object>> myPostList(String userId) {
		return postDAO.myPostList(userId);
	}

	@Override
	public void deletePost(long postId) {
		postDAO.deletePost(postId);
	}

	@Override
	public void updatePost(PostDTO postDTO) {
		postDAO.updatePost(postDTO);
	}

	@Override
	public Long getPrevPostId(long postId, long categoryId) {
		return postDAO.getPrevPostId(postId, categoryId);
	}

	@Override
	public Long getNextPostId(long postId, long categoryId) {
		return postDAO.getNextPostId(postId, categoryId);
	}

	@Override
	public List<Map<String, Object>> getPostListByCategory(Long categoryId, int pageSize, int offset) {
		return postDAO.getPostListByCategory(categoryId, pageSize, offset);
	}

	@Override
	public long getPostCntByCategory(Long categoryId) {
		long count = postDAO.getPostCntByCategory(categoryId);
		return Math.max(count, 0); // 0 미만이 되지 않도록 보장
	}

	@Override
	public String getCategoryName(Long categoryId) {
		return postDAO.getCategoryName(categoryId);
	}

}
