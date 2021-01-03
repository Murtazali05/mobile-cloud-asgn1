/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class VideoController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long, Video> videos = new HashMap<>();

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody
	Collection<Video> getVideoList(){
		return videos.values();
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideoMetadata(@RequestBody Video video, HttpServletRequest request){
		checkAndSetId(video);
		video.setDataUrl(getUrlBaseForLocalServer(request) + "/video/" + video.getId() + "/data");
		videos.put(video.getId(), video);
		return video;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody
	VideoStatus addVideoData(@PathVariable("id") long id, @RequestParam MultipartFile data) throws IOException {
		VideoFileManager fileManager = VideoFileManager.get();
		if (!videos.containsKey(id)) {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find resource");
		} else {
			try {
				fileManager.saveVideoData(videos.get(id), data.getInputStream());
			} catch ( IOException e) {
				throw new ResponseStatusException(NOT_FOUND, "Unable to find resource");
			}
			return new VideoStatus(VideoStatus.VideoState.READY);
		}
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public ResponseEntity<OutputStream> getData(@PathVariable("id") long id, HttpServletResponse response) throws IOException{
		VideoFileManager fileManager = VideoFileManager.get();
		OutputStream out = response.getOutputStream();

		if (!videos.containsKey(id)) {
			return ResponseEntity.notFound().build();
		} else {
			fileManager.copyVideoData(videos.get(id), out);
			out.flush();
			out.close();
			return ResponseEntity.ok().body(out);
		}
	}

	private String getUrlBaseForLocalServer(HttpServletRequest request) {
		return "http://"+request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
}
