package com.application.foodhub.post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.application.foodhub.bookmark.BookmarkService;
import com.application.foodhub.comment.CommentService;
import com.application.foodhub.fileUpload.FileUploadDTO;
import com.application.foodhub.fileUpload.FileUploadService;
import com.application.foodhub.postLike.PostLikeDTO;
import com.application.foodhub.postLike.PostLikeService;
import com.application.foodhub.postReport.PostReportDTO;
import com.application.foodhub.postReport.PostReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/foodhub/post")
public class PostController {

	@Autowired
	private PostService postService;

	@Autowired
	private FileUploadService fileUploadService; // 파일 업로드 서비스 추가

	@Autowired
	private PostLikeService postLikeService;

	@Autowired
	private PostReportService postReportService;

	@Autowired
	private BookmarkService bookmarkService;

	
	@GetMapping("/posts")
	public String listPosts(
	    @RequestParam(name = "page", defaultValue = "1") int page,
	    @RequestParam(name = "searchType", required = false) String searchType,
	    @RequestParam(name = "keyword", required = false) String keyword,
	    @RequestParam(name = "categoryId", required = false) Long categoryId,
	    @RequestParam(name = "subCateId", required = false) Long subCateId,
	    @RequestParam(name = "orderType", required = false, defaultValue = "newest") String orderType,  // 최신순(default) 또는 추천순(best)
	    Model model
	) {
	    final int pageSize = 15;
	    final int pageGroupSize = 5;
	    int offset = (page - 1) * pageSize;

	    // ✅ 게시글 개수 조회
	    long totalPosts = postService.countPosts(keyword, searchType, categoryId, subCateId);
	    List<Map<String, Object>> postList = postService.getPostList(categoryId, subCateId, orderType, keyword, searchType, pageSize, offset);

	    // ✅ 페이지네이션 계산
	    int maxPages = (int) Math.ceil((double) totalPosts / pageSize);
	    if (maxPages == 0) maxPages = 1;
	    int startPage = ((page - 1) / pageGroupSize) * pageGroupSize + 1;
	    int endPage = Math.min(startPage + pageGroupSize - 1, maxPages);

	    // ✅ 카테고리 및 서브카테고리 이름 가져오기
	    String categoryName = (categoryId != null) ? postService.getCategoryNameById(categoryId) : null;
	    String subCateNm = (subCateId != null) ? postService.getSubCateNameById(subCateId) : null;

	    // ✅ 모델에 데이터 추가
	    model.addAttribute("categoryName", categoryName);
	    model.addAttribute("categoryId", categoryId);
	    model.addAttribute("subCateNm", subCateNm); 
	    model.addAttribute("subCateId", subCateId);
	    model.addAttribute("postListMap", postList);
	    model.addAttribute("page", page);
	    model.addAttribute("maxPages", maxPages);
	    model.addAttribute("startPage", startPage);
	    model.addAttribute("endPage", endPage);
	    model.addAttribute("searchType", searchType);
	    model.addAttribute("keyword", keyword);
	    model.addAttribute("orderType", orderType);

	    // ✅ View 선택 (일반, 카테고리, 베스트, 서브 카테고리)
	    if (subCateId != null) {
	        return "foodhub/post/subCategoryPostList";
	    } else if (categoryId != null) {
	        return "foodhub/post/categoryPostList";
	    } else if ("best".equals(orderType)) {
	        return "foodhub/post/bestPostList";
	    } else {
	        return "foodhub/post/allPostList";
	    }
	}


	@GetMapping("/createPost")
	public String createPost(HttpServletRequest request) {

		HttpSession session = request.getSession();
		String userId = (String) session.getAttribute("userId");

		// 로그인 상태가 아니라면 로그인 페이지로 리다이렉트
		if (userId == null) {
			return "redirect:/foodhub/user/login";
		}

		return "foodhub/post/createPost";
	}

	@PostMapping("/createPost")
	@ResponseBody
	public String createPost(@RequestParam("file[]") List<MultipartFile> files, @ModelAttribute PostDTO postDTO,
			HttpSession session) {

		String userId = (String) session.getAttribute("userId");
		postDTO.setUserId(userId);

		String nickname = (String) session.getAttribute("nickname");
		// System.out.println("세션 닉네임: " + nickname);

		Long postId = postService.createPost(postDTO); // ✅ 게시글 저장 후 postId를 받아옴
		postDTO.setPostId(postId); // ✅ postDTO에도 설정

		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				FileUploadDTO fileUploadDTO = new FileUploadDTO();
				fileUploadDTO.setPostId(postId);
				try {
					fileUploadService.uploadFile(file, fileUploadDTO);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		String jsScript = """
				<script>
					alert('커뮤니티 게시글이 작성 되었습니다.');
					location.href = 'posts';
				</script>""";

		return jsScript;
	}

	/**
	 * 게시글 상세보기
	 */
	@GetMapping("/postDetail")
	public String postDetail(Model model,
							@RequestParam(value = "postId", required = false, defaultValue = "1") long postId,
							HttpSession session) {
		
		// 게시글 상세 정보 가져오기
		Map<String, Object> postMap = postService.getPostDetail(postId, true);
		model.addAttribute("postMap", postMap);

		if (postMap == null || "DELETED".equals(postMap.get("status"))) {
			return "foodhub/post/deletedPost404"; 
		}

		// 해당 게시글의 파일 목록 가져오기
		List<FileUploadDTO> fileList = fileUploadService.getFileListByPostId(postId);
		model.addAttribute("fileList", fileList);

		// 게시글의 카테고리 ID 가져오기
		Long categoryId = (long) postMap.get("categoryId");
		// System.out.println("카테고리 아이디 : " + categoryId);

		// 이전 글, 다음 글의 postId 가져오기
		Long prevPostId = postService.getPrevPostId(postId, categoryId);
		Long nextPostId = postService.getNextPostId(postId, categoryId);
		model.addAttribute("prevPostId", prevPostId);
		model.addAttribute("nextPostId", nextPostId);
		
		// 로그인한 사용자 ID 가져오기
	    String userId = (String) session.getAttribute("userId");
	    
	    // 북마크 여부 확인
	    boolean isBookmarked = false;
	    if (userId != null) {
	        isBookmarked = bookmarkService.isBookmarked(userId, postId);
	    }
	    
	    model.addAttribute("isBookmarked", isBookmarked);

		return "foodhub/post/postDetail";
	}


	// 게시글 삭제
	@GetMapping("/deletePost")
	public String deletePost(Model model, @RequestParam("postId") long postId, HttpServletRequest request) {

		HttpSession session = request.getSession();

		String sessionNickname = (String) session.getAttribute("nickname");

		// 로그인 상태가 아니라면 로그인 페이지로 리다이렉트
		if (sessionNickname == null) {
			return "redirect:/foodhub/user/login";
		}

		model.addAttribute("postId", postId);
		model.addAttribute("postMap", postService.getPostDetail(postId, false));
		model.addAttribute("sessionNickname", sessionNickname);

		return "foodhub/post/deletePost";
	}

	@PostMapping("/deletePost")
	@ResponseBody
	public String deletePost(@RequestParam("postId") Long postId) {
	postService.markPostAsDeleted(postId);

		String jsScript = """
				<script>
					alert('게시글이 삭제 되었습니다.');
					location.href='/foodhub';
				</script>

				""";

		return jsScript;
	}

	// 게시글 수정
	@GetMapping("/updatePost")
	public String updatePost(Model model, @RequestParam("postId") long postId, HttpServletRequest request) {

		HttpSession session = request.getSession();
		String sessionNickname = (String) session.getAttribute("nickname");

		// 게시글 정보 가져오기
		Map<String, Object> postMap = postService.getPostDetail(postId, false);
		String postNickname = (String) postMap.get("nickname");

		// 현재 로그인한 사용자가 게시글 작성자가 아니라면 접근 차단
		if (sessionNickname == null || !sessionNickname.equals(postNickname)) {
			return "redirect:/foodhub/post/postDetail?postId=" + postId;
		}

		// 기존 첨부파일 목록 가져오기
		List<FileUploadDTO> fileList = fileUploadService.getFileListByPostId(postId);

//	    // 파일 목록이 제대로 불러와지는지 로그 출력
//	    System.out.println("파일 개수: " + fileList.size());
//	    for (FileUploadDTO file : fileList) {
//	        System.out.println("파일명: " + file.getFileName() + ", UUID: " + file.getFileUUID());
//	    }

		// 모델에 데이터 추가
		model.addAttribute("postId", postId);
		model.addAttribute("postMap", postMap);
		model.addAttribute("sessionNickname", sessionNickname);
		model.addAttribute("fileList", fileList); // 기존 파일 목록 추가

		return "foodhub/post/updatePost";
	}

	@PostMapping("/updatePost")
	@ResponseBody
	public String updatePost(@RequestParam("file[]") List<MultipartFile> files,
			@RequestParam(value = "deleteFiles", required = false) List<String> deleteFiles,
			@ModelAttribute PostDTO postDTO, HttpSession session) {

		String userId = (String) session.getAttribute("userId");
		postDTO.setUserId(userId);

		String jsScript = """

				""";

		try {
			// 게시글 업데이트 실행
			postService.updatePost(postDTO);
			Long postId = postDTO.getPostId();

			// ✅ 기존 파일 삭제 (체크된 파일만 삭제)
			if (deleteFiles != null && !deleteFiles.isEmpty()) {
				for (String fileUUID : deleteFiles) {
					fileUploadService.deleteFileByUUID(fileUUID);
				}
			}

			// ✅ 새로운 파일 업로드
			for (MultipartFile file : files) {
				if (!file.isEmpty()) {
					FileUploadDTO fileUploadDTO = new FileUploadDTO();
					fileUploadDTO.setPostId(postId);
					fileUploadService.uploadFile(file, fileUploadDTO);
				}
			}
			jsScript = """
					<script>
					alert('게시글이 성공적으로 수정되었습니다.');
					location.href='/foodhub/post/postDetail?postId=' + """ + postId + """
					</script>
					""";

		} catch (Exception e) {
			e.printStackTrace();
			jsScript = """
					<script>
					alert('게시글 수정 중 오류가 발생했습니다.');
					location.href='/foodhub/post/postDetail?postId=' + """ + postDTO.getPostId() + """
					</script>
							""";

		}
		return jsScript;
	}

	@PostMapping("/postLike")
	public ResponseEntity<Map<String, Object>> postLike(@RequestBody PostLikeDTO postLikeDTO) {

		long postId = postLikeDTO.getPostId();
		String userId = postLikeDTO.getUserId();

//	    System.out.println("postId : " + postId);
//	    System.out.println("userId : " + userId);

		boolean isLiked = postLikeService.togglePostLike(postId, userId);
		int likeCount = postLikeService.getPostLikeCount(postId);
		
		Map<String, Object> result = new HashMap<>();
		
		result.put("isLiked", isLiked);
		result.put("likeCount", likeCount);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/report")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> report(@RequestBody PostReportDTO postReportDTO) {

		long postId = postReportDTO.getPostId();
		String userId = postReportDTO.getUserId();
		String content = postReportDTO.getContent();

//		System.out.println(postId);
//		System.out.println(userId);
//		System.out.println(content);

		boolean reportSuccess = postReportService.reportPost(postId, userId, content);

		Map<String, Object> response = new HashMap<>();

		// 이미 신고한 경우
		if (!reportSuccess) {
			response.put("success", false);
			response.put("message", "이미 신고한 게시글입니다.");
			response.put("redirectUrl", "/foodhub/post/postDetail?postId=" + postReportDTO.getPostId());
			return ResponseEntity.ok(response); // 🚨 클라이언트가 알 수 있도록 JSON 반환
		} else {
			// 신고 성공 시
			response.put("success", true);
			response.put("message", "게시글이 신고되었습니다.");
			response.put("redirectUrl", "/foodhub/post/postDetail?postId=" + postReportDTO.getPostId());

			return ResponseEntity.ok(response);
		}

	}

	// 북마크
	@PostMapping("/toggleBookmark")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> toggleBookmark(@RequestBody Map<String, Object> requestData,
			HttpSession session) {
		String userId = (String) session.getAttribute("userId");
		Long postId = Long.valueOf(requestData.get("postId").toString());

		boolean isBookmarked = bookmarkService.toggleBookmark(userId, postId);

		Map<String, Object> response = new HashMap<>();
		response.put("isBookmarked", isBookmarked);
		response.put("message", isBookmarked ? "북마크가 추가되었습니다." : "북마크가 삭제되었습니다.");

		return ResponseEntity.ok(response);
	}
	



}
