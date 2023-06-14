package com.revature.PureDataBase2.controllers;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.revature.PureDataBase2.entities.PdLibrary;
import com.revature.PureDataBase2.entities.PdObject;
import com.revature.PureDataBase2.entities.Like;
import com.revature.PureDataBase2.entities.User;
import com.revature.PureDataBase2.services.JWTService;
import com.revature.PureDataBase2.services.UserService;
import com.revature.PureDataBase2.services.LikeService;
import com.revature.PureDataBase2.services.PdLibraryService;
import com.revature.PureDataBase2.util.custom_exceptions.ResourceConflictException;
import com.revature.PureDataBase2.DTO.responses.LikeResult;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/like")
public class LikeController {
    // dependency injection ie. services
    private final JWTService tokenService;
    private final UserService userService;
    private final LikeService likeService;
    private final PdLibraryService libraryService;

    @GetMapping
    public ResponseEntity<List<LikeResult>> getAllUserLikes(HttpServletRequest req) {
        // return all likes
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        List<Like> likeList = likeService.getAllForUser(userId);
        List<LikeResult> resultList = new ArrayList<LikeResult>();
        PdLibrary library;
        PdObject object;
        for (Like like : likeList) {
            switch(like.getEntityType()) {
                case LIBRARY:
                    library = libraryService.getById(like.getEntityId());
                    resultList.add(new LikeResult("library", library.getName()));
                    break;
                case OBJECT:
                    object = libraryService.getObjectByObjectId(like.getEntityId());
                    resultList.add(new LikeResult("object",
                        object.getLibrary().getName() + '/' + object.getName()));
                    break;
                case AUTHOR:
                    resultList.add(new LikeResult("author", like.getEntityId()));
                    break;
                default: throw new ResourceConflictException("like without subject");
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(resultList);
    }

    @PostMapping("/object/{libraryName}/{objectName}")
    public ResponseEntity<?> createObjectLike(@PathVariable String libraryName, @PathVariable String objectName,
            HttpServletRequest req) {
        // only users can like
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        User user = userService.getById(userId);
        String objectId = libraryService.getObjectByNameAndLibraryName(objectName, libraryName).getId();
        if(likeService.hasUserLikedEntity(objectId, userId))
            throw new ResourceConflictException("already liked " + libraryName + '/' + objectName);
        Like like = new Like(Like.EntityType.OBJECT, objectId, user);
        likeService.create(like);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/library/{libraryName}")
    public ResponseEntity<?> createLibraryLike(@PathVariable String libraryName,
            HttpServletRequest req) {
        // only users can like
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        User user = userService.getById(userId);
        String libraryId = libraryService.getByName(libraryName).getId();
        if(likeService.hasUserLikedEntity(libraryId, userId))
            throw new ResourceConflictException("already liked " + libraryName);
        Like like = new Like(Like.EntityType.LIBRARY, libraryId, user);
        likeService.create(like);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/author/{authorName}")
    public ResponseEntity<?> createAuthorLike(@PathVariable String authorName, HttpServletRequest req) {
        // only users can like
        // just use author name as id
        authorName = authorName.replace("%20", " ");
        authorName = authorName.replace('+', ' ');
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        User user = userService.getById(userId);
        if(likeService.hasUserLikedEntity(authorName, userId))
            throw new ResourceConflictException("already liked " + authorName);
        Like like = new Like(Like.EntityType.AUTHOR, authorName, user);
        likeService.create(like);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/object/{libraryName}/{objectName}")
    public ResponseEntity<?> deleteObject(@PathVariable String libraryName, @PathVariable String objectName,
            HttpServletRequest req) {
        // only users can like
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        String objectId = libraryService.getObjectByNameAndLibraryName(objectName, libraryName).getId();
        if(!likeService.hasUserLikedEntity(objectId, userId))
            throw new ResourceConflictException(libraryName + '/' + objectName + " isn't liked");
        likeService.deleteByEntityId(objectId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/library/{libraryName}")
    public ResponseEntity<?> deleteLibraryLike(@PathVariable String libraryName,
            HttpServletRequest req) {
        // only users can like
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        String libraryId = libraryService.getByName(libraryName).getId();
        if(likeService.hasUserLikedEntity(libraryId, userId))
            throw new ResourceConflictException(libraryName + " isn't liked");
        likeService.deleteByEntityId(libraryId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/author/{authorName}")
    public ResponseEntity<?> deleteAuthorLike(@PathVariable String authorName, HttpServletRequest req) {
        // only users can like
        // just use author name as id
        authorName = authorName.replace("%20", " ");
        authorName = authorName.replace('+', ' ');
        String userId = tokenService.extractUserId(req.getHeader("auth-token")); 
        if(likeService.hasUserLikedEntity(authorName, userId))
            throw new ResourceConflictException(authorName + " isn't liked");
        likeService.deleteByEntityId(authorName, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
