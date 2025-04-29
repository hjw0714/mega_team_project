		package com.application.foodhub.chat;
		
		import java.util.List;
		import java.util.Map;
		
		import org.springframework.beans.factory.annotation.Autowired;
		import org.springframework.messaging.handler.annotation.DestinationVariable;
		import org.springframework.messaging.handler.annotation.MessageMapping;
		import org.springframework.messaging.handler.annotation.Payload;
		import org.springframework.messaging.handler.annotation.SendTo;
		import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
		import org.springframework.messaging.simp.SimpMessageSendingOperations;
		import org.springframework.stereotype.Controller;
		import org.springframework.ui.Model;
		import org.springframework.web.bind.annotation.GetMapping;
		import org.springframework.web.bind.annotation.PathVariable;
		import org.springframework.web.bind.annotation.PostMapping;
		import org.springframework.web.bind.annotation.RequestBody;
		import org.springframework.web.bind.annotation.RequestParam;
		import org.springframework.web.bind.annotation.ResponseBody;
		import org.springframework.web.bind.annotation.SessionAttribute;

import com.application.foodhub.config.JwtUtil;
import com.application.foodhub.user.UserDTO;
		import com.application.foodhub.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
		
		
		@Controller
		public class ChatController {
		
			@Autowired
			private ChatRoomService chatRoomService;
			
			@Autowired
			private UserService userService;
			
			@Autowired
			private SimpMessageSendingOperations messagingTemplate;
		
			@Autowired
			private JwtUtil jwtUtil;
			
			@GetMapping("/foodhub/chat/chat")
			public String chatRoom(Model model, @SessionAttribute(name = "userDTO", required = false) UserDTO userDTO) {
			   
				if (userDTO != null) {
			        model.addAttribute("userDTO", userDTO);
			    }
			    return "foodhub/chat/chat"; // chat.html의 위치
			}
			
		    @MessageMapping("/chat.sendMessage")
		    @SendTo("/topic/public")
		    public ChatMessage sendMessage(
		            @Payload ChatMessage chatMessage
		    ) {
		        return chatMessage;
		    }
		
		    @MessageMapping("/chat.addUser")
		    @SendTo("/topic/public")
		    public ChatMessage addUser(
		            @Payload ChatMessage chatMessage,
		            SimpMessageHeaderAccessor headerAccessor
		    ) {
		        // Add username in web socket session
		        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
		        return chatMessage;
		    }
		    
		    
		    @MessageMapping("/chat.private.{roomId}")
		    public void sendPrivateMessage(@DestinationVariable("roomId") String roomId,
		                                   @Payload ChatMessage message) {
		        String senderId = message.getSender();
		        Long roomIdLong = Long.parseLong(roomId);
		        System.out.println(senderId);
		        System.out.println(roomIdLong);
		        
		        String receiverId = chatRoomService.findOtherUserId(roomIdLong, senderId);
		        System.out.println(receiverId);
		        if (receiverId == null) {
		            System.err.println("❌ 수신자를 찾을 수 없습니다.");
		            return;
		        }

		        // ✅ 닉네임 조회 및 설정
		        String senderNickname = userService.findNicknameByUserId(senderId);
		        message.setSenderNickname(senderNickname);

		        message.setReceiver(receiverId);

		        ChatMessageDTO dto = new ChatMessageDTO();
		        dto.setRoomId(roomIdLong);
		        dto.setSenderId(senderId);
		        dto.setReceiveId(receiverId);
		        dto.setChatContent(message.getContent());
		        chatRoomService.saveMessage(dto);

		        messagingTemplate.convertAndSend("/topic/private." + roomId, message);
		    }


		    
		    // ✅ 비공개 채팅방 리스트 AJAX 요청 처리
		    @GetMapping("/foodhub/chat/private/list")
		    @ResponseBody
		    public List<ChatRoomDTO> getMyPrivateChatRoomList(
		            @SessionAttribute(name = "userDTO", required = false) UserDTO userDTO
		    ) {
		        if (userDTO == null) {		   
		            return List.of(); // 로그인 안 된 경우 빈 리스트 반환
		        }
		        List<ChatRoomDTO> roomList = chatRoomService.getMyPrivateRooms(userDTO.getUserId());
		        for (ChatRoomDTO chatRoomDTO : roomList) {
					System.out.println(chatRoomDTO);
				}
		        return roomList;
		    }
		    
		    
		    
		    @PostMapping("/foodhub/chat/private/create")
		    @ResponseBody
		    public ChatRoomDTO createPrivateChatRoom(
		            @RequestBody Map<String, String> request,
		            @SessionAttribute(name = "userDTO", required = false) UserDTO currentUser
		    ) {
		        if (currentUser == null) {
		            throw new IllegalStateException("로그인이 필요합니다.");
		        }

		        String targetNickname = request.get("nickname");

		        UserDTO targetUser = userService.findByNickname(targetNickname);
		        if (targetUser == null) {
		            throw new IllegalArgumentException("해당 닉네임을 가진 유저를 찾을 수 없습니다.");
		        }

		        if (targetUser.getUserId().equals(currentUser.getUserId())) {
		            throw new IllegalArgumentException("자기 자신과 채팅방을 생성할 수 없습니다.");
		        }

		        return chatRoomService.createPrivateRoom(currentUser.getUserId(), targetUser.getUserId());
		    }

		    
		    @GetMapping("/chat/private/room/{roomId}")
		    public String enterPrivateChatRoom(
		            @PathVariable("roomId") Long roomId,
		            Model model,
		            @SessionAttribute(name = "userDTO", required = false) UserDTO userDTO
		    ) {
		        if (userDTO == null) {
		            return "redirect:/login";  // 또는 에러 페이지
		        }
		
		        model.addAttribute("roomId", roomId);
		        model.addAttribute("userDTO", userDTO);
		        
		        List<ChatMessageDTO> messageList = chatRoomService.getChatMessages(roomId);
		        model.addAttribute("messageList", messageList);

		
		        return "foodhub/chat/chat";  // ← chatPrivate.html이 존재해야 함
		    }
		
		    
		    @GetMapping("/chat/private/messages/{roomId}")
		    @ResponseBody
		    public List<ChatMessageDTO> getChatMessages(@PathVariable("roomId") Long roomId) {
		        return chatRoomService.getChatMessages(roomId);
		    }
		    
		    @PostMapping("/foodhub/chat/private/delete/{roomId}") // 채팅방 삭제
		    @ResponseBody
		    public String deletePrivateChatRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
		        String userId = (String) session.getAttribute("userId");
		        chatRoomService.deleteChatRoomForUser(roomId, userId);
		        
		        String nickname = userService.findNicknameByUserId(userId); // 닉네임 조회
		        ChatMessage systemMessage = new ChatMessage();
		        systemMessage.setSender("SYSTEM");
		        systemMessage.setType(MessageType.LEAVE);
		        systemMessage.setContent(nickname + " 님이 채팅방에서 나갔습니다.");

		        messagingTemplate.convertAndSend("/topic/private." + roomId, systemMessage);

		        
		        return "ok";
		    }
		    
		   
		    
		    @GetMapping("/foodhub/admin/chat/private/list") // 관리자 화면 채팅방 리스트
		    @ResponseBody
		    public List<ChatRoomDTO> getMyPrivateChatRoomListForAdmin (HttpServletRequest request) {
		    	
		    	String header = request.getHeader("Authorization");
		    	 if (header == null || !header.startsWith("Bearer ")) {
		    		 throw new RuntimeException("요청 정보가 부족합니다.");
		         }
		    	String token = header.substring(7);
		    	System.out.println("header : " + header);
		    	System.out.println("token : " + token);

		        try {
		            
		        	String adminId = jwtUtil.validateToken(token); 
		        	System.out.println("adminId : " + adminId);
		        	
		            List<ChatRoomDTO> roomList = chatRoomService.getMyPrivateRooms(adminId);
		            for (ChatRoomDTO chatRoomDTO : roomList) {
		                System.out.println(chatRoomDTO);
		            }

		            return roomList;

		        } catch (Exception e) {
		            throw new RuntimeException("인증 실패: " + e.getMessage());
		        } 
		    
		    }
		    
		    
		    @GetMapping("/foodhub/admin/chat/private/messages/{roomId}") // 관리자 화면에서 채팅내역 불러오기
		    @ResponseBody
		    public List<ChatMessageDTO> getMessagesForAdmin(@PathVariable("roomId") Long roomId, HttpServletRequest request) {
		    	
		    	String header = request.getHeader("Authorization");
		    	 if (header == null || !header.startsWith("Bearer ")) {
		    		 throw new RuntimeException("요청 정보가 부족합니다.");
		         }
		    	String token = header.substring(7);

		    	  try {
			        	String adminId = jwtUtil.validateToken(token); 
			        	System.out.println("adminId : " + adminId);

			            return chatRoomService.getChatMessages(roomId);

			        } catch (Exception e) {
			            throw new RuntimeException("인증 실패: " + e.getMessage());
			        } 
		       
		    }
		    
		    
		    @PostMapping("/foodhub/admin/chat/private/delete/{roomId}") // 관리자 화면 채팅방 삭제
		    @ResponseBody
		    public String deletePrivateChatRoomAdmin(@PathVariable("roomId") Long roomId, HttpServletRequest request) {
		    	String header = request.getHeader("Authorization");
		    	 if (header == null || !header.startsWith("Bearer ")) {
		    		 throw new RuntimeException("요청 정보가 부족합니다.");
		         }
		    	String token = header.substring(7);

		    	  try {
			        	String userId = jwtUtil.validateToken(token); 
			        	chatRoomService.deleteChatRoomForUser(roomId, userId);
			        	
			        	 // ✅ 시스템 메시지 전송
			            String nickname = userService.findNicknameByUserId(userId);
			            ChatMessage systemMessage = new ChatMessage();
			            systemMessage.setSender("SYSTEM");
			            systemMessage.setType(MessageType.LEAVE);
			            systemMessage.setContent(nickname + " 님이 채팅방에서 나갔습니다.");

			            messagingTemplate.convertAndSend("/topic/private." + roomId, systemMessage);
			        	return "ok";
		    	  } catch (Exception e) {
		    		  throw new RuntimeException("인증 실패: " + e.getMessage());
		    	  }
		    }
		    
		    
		    
		    @PostMapping("/foodhub/admin/chat/private/create")
		    @ResponseBody
		    public ChatRoomDTO createPrivateChatRoom(@RequestBody Map<String, String> map,
		    										 HttpServletRequest request) {
		      
		    	String header = request.getHeader("Authorization");
		    	 if (header == null || !header.startsWith("Bearer ")) {
		    		 throw new RuntimeException("요청 정보가 부족합니다.");
		         }
		    	String token = header.substring(7);
		    	
		    	try {
		    		String userId = jwtUtil.validateToken(token); 
		    		
		    		String targetNickname = map.get("nickname");
		    		UserDTO targetUser = userService.findByNickname(targetNickname);
		    		
		    		if (targetUser == null) {
		    			throw new IllegalArgumentException("해당 닉네임을 가진 유저를 찾을 수 없습니다.");
		    		}
		    		 if (targetUser.getUserId().equals(userId)) {
				            throw new IllegalArgumentException("자기 자신과 채팅방을 생성할 수 없습니다.");
				        }
		    		 return chatRoomService.createPrivateRoom(userId, targetUser.getUserId());
					
				} catch (Exception e) {
					 throw new RuntimeException("인증 실패: " + e.getMessage());
				}
		       
		    }
		    
		    
}
		    
