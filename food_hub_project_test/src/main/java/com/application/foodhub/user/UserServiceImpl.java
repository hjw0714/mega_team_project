package com.application.foodhub.user;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

	@Value("${file.repo.path}")
	private String fileRepositoryPath;
	@Autowired
	private UserDAO userDAO;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public void register(UserDTO userDTO) {
		
		if (userDTO.getEmailYn() == null)
			userDTO.setEmailYn("N");
		if (userDTO.getSmsYn() == null)
			userDTO.setSmsYn("N");

		userDTO.setPasswd(passwordEncoder.encode(userDTO.getPasswd()));
		
		userDAO.register(userDTO);
	}

	@Override // 아이디 중복확인
	public String checkValidId(String userId) {

		String isValidId = "n";
		if (userDAO.checkValidId(userId) == null) {
			isValidId = "y";
		}

		return isValidId;
	}

	@Override
	public String checkValidNickname(String nickname) {
		String isValidNickname = "n";
		if (userDAO.checkValidNickname(nickname) == null) {
			isValidNickname = "y";
		}

		return isValidNickname;
	}

	@Override
	public String checkValidEmail(String email) {
		String isValidEmail = "n";
		if (userDAO.checkValidEmail(email) == null) {
			isValidEmail = "y";
		}

		return isValidEmail;
	}

	@Override
	public boolean login(UserDTO userDTO) {

		// System.out.println("입력된 userId: " + userDTO.getUserId());

		String encodedPasswd = userDAO.getEncodedPasswd(userDTO.getUserId());

		if (passwordEncoder.matches(userDTO.getPasswd(), encodedPasswd)) {
			return true;
		}

		return false;
	}

	@Override
	public UserDTO getUserDetail(String userId) {
		return userDAO.getUserDetail(userId);
	}

	@Override
	public void updateUser(MultipartFile uploadProfile, UserDTO userDTO) throws IllegalStateException, IOException {
		// 기존 프로필 사진 유지
		if (uploadProfile == null || uploadProfile.isEmpty()) {
			UserDTO existingUser = userDAO.getUserDetail(userDTO.getUserId());
			userDTO.setProfileUUID(existingUser.getProfileUUID());
			userDTO.setProfileOriginal(existingUser.getProfileOriginal());
		} else {
			// 기존 파일 삭제
			if (userDTO.getProfileUUID() != null) {
				new File(fileRepositoryPath + userDTO.getProfileUUID()).delete();
			}

			// 새로운 파일 저장
			String originalFilename = uploadProfile.getOriginalFilename();
			userDTO.setProfileOriginal(originalFilename);

			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			String uploadFile = UUID.randomUUID() + extension;
			userDTO.setProfileUUID(uploadFile);

			uploadProfile.transferTo(new File(fileRepositoryPath + uploadFile));
		}

		// 일반 회원으로 변경 시 창업일과 업종 null 처리
		if ("COMMON".equals(userDTO.getMembershipType())) {
			userDTO.setFoundingAt(null); // 창업일 null
			userDTO.setBusinessType(null); // 업종 null
		}

		// 사용자 정보 업데이트
		userDAO.updateUser(userDTO);
	}

	@Override
	@Transactional
	public void deleteUser(String userId) {

		String deleteProfile = userDAO.getDeleteUserProfile(userId);
		new File(fileRepositoryPath + deleteProfile).delete();
		userDAO.deleteUser(userId);
	}

	@Override
	public String findId(String email, String tel) {

		Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		params.put("tel", tel);

		String userId = userDAO.findId(params);

		if (userId == null) {
			userId = "notFound";
		}
		return userId;
	}

	@Override
	public String findPasswd(String userId, String email, String tel) {

		Map<String, Object> params = new HashMap<>();

		params.put("userId", userId);
		params.put("email", email);
		params.put("tel", tel);

		String userPasswd = userDAO.findPasswd(params);

		if (userPasswd == null) {
			userPasswd = "notFound";
		}

		return userPasswd;
	}

	@Override
	public void resetPassword(String newPassword, String userId) {

//	    System.out.println(newPassword);
//	    System.out.println(userId);
//	    System.out.println(passwordEncoder.encode(newPassword));

		UserDTO userDTO = new UserDTO();
		userDTO.setUserId(userId);
		userDTO.setPasswd(passwordEncoder.encode(newPassword));

		// System.out.println("변경된 비밀번호: " + userDTO.getPasswd()); // 확인용 출력

		userDAO.resetPassword(userDTO);
	}

	@Override
	public String findNicknameByUserId(String userId) {
		return userDAO.findNicknameByUserId(userId); // ✅ DAO를 통해 닉네임 조회
	}

}
