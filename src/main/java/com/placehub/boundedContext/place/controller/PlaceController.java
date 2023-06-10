package com.placehub.boundedContext.place.controller;

import com.placehub.base.rq.Rq;
import com.placehub.base.util.LocalApi;
import com.placehub.base.util.PlaceData;
import com.placehub.boundedContext.category.entity.BigCategory;
import com.placehub.boundedContext.category.entity.MidCategory;
import com.placehub.boundedContext.category.entity.SmallCategory;
import com.placehub.boundedContext.category.service.BigCategoryService;
import com.placehub.boundedContext.category.service.MidCategoryService;
import com.placehub.boundedContext.category.service.SmallCategoryService;
import com.placehub.boundedContext.place.PlaceInfo;
import com.placehub.boundedContext.place.entity.Place;
import com.placehub.boundedContext.place.service.PlaceService;
import com.placehub.boundedContext.placelike.entity.PlaceLike;
import com.placehub.boundedContext.placelike.service.PlaceLikeService;
import com.placehub.boundedContext.post.entity.Post;
import com.placehub.boundedContext.post.form.Viewer;
import com.placehub.boundedContext.post.service.PostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/place")
public class PlaceController {
    private final PlaceService placeService;
    private final BigCategoryService bigCategoryService;
    private final MidCategoryService midCategoryService;
    private final SmallCategoryService smallCategoryService;
    private final PlaceLikeService placeLikeService;
    private final PostService postService;
    private final PlaceData placeData;
    private final Rq rq;

    @Data
    public class SearchForm {
        private Double latitude;
        private Double longitude;
        private Long bigCategoryId;
        private Long midCategoryId;
    }

    @GetMapping("/search")
    public String search(Model model,
                         @RequestParam(value = "longitude", required = false) Double longitude,
                         @RequestParam(value = "latitude", required = false) Double latitude,
                         @RequestParam(value = "bigCategoryId", required = false) Long bigCategoryId,
                         @RequestParam(value = "midCategoryId", required = false) Long midCategoryId,
                         @RequestParam(value = "smallCategoryId", required = false) Long smallCategoryId) {

        List<Place> placeList = null;
        List<BigCategory> bigCategories = bigCategoryService.findAll();
        List<MidCategory> midCategories = midCategoryService.findAll();
        List<SmallCategory> smallCategories = smallCategoryService.findAll();
        model.addAttribute("bigCategories", bigCategories);
        model.addAttribute("midCategories", midCategories);
        model.addAttribute("smallCategories", smallCategories);

        // 위치 처리
        if (longitude == null && latitude == null) {
            return "usr/place/search";
        } else {
            Coordinate coord = new Coordinate(longitude, latitude);
            GeometryFactory factory = new GeometryFactory();
            Point point = factory.createPoint(coord);

            placeList = placeService.findPlaceBySpecificDistance(point, 2000L);
        }

        // 카테고리 처리
        if (bigCategoryId != null) {
            placeList = placeList.stream()
                    .filter(place -> place.getBigCategoryId() == bigCategoryId)
                    .collect(Collectors.toList());
        }
        if (midCategoryId != null) {
            placeList = placeList.stream()
                    .filter(place -> place.getMidCategoryId() == midCategoryId)
                    .collect(Collectors.toList());
        }

        if (smallCategoryId != null) {
            placeList = placeList.stream()
                    .filter(place -> place.getSmallCategoryId() == smallCategoryId)
                    .collect(Collectors.toList());
        }

        List<PlaceInfo> placeInfoList = placeService.getCategoryNamesList(placeList);
        model.addAttribute("placeInfoList", placeInfoList);

        return "usr/place/search";
    }
    @GetMapping("/details/{placeId}")
    public String details(Model model, @PathVariable("placeId") Long id) {
        Place place = placeService.getPlace(id);
        if (place == null) {
            throw new RuntimeException("해당 장소는 없습니다.");
        }
        model.addAttribute("place", place);

        PlaceInfo placeInfo = placeService.getCategoryNames(place);
        model.addAttribute("placeInfo", placeInfo);

        if (rq.getMember() != null) {
            PlaceLike placeLike = placeLikeService.findByPlaceIdAndMemberId(id, rq.getMember().getId());
            model.addAttribute("placeLike", placeLike);
        }

        List<Post> postList = postService.findByPlace(id);
        List<Viewer> postViewerList = new ArrayList<>();
        for (Post post : postList) {
            postViewerList.add(postService.showSinglePost(post.getId()).getData());
        }

        model.addAttribute("postList", postViewerList);

        return "usr/place/details";
    }


    @GetMapping("/keyWordTest/{keyWord}/{radius}")
    @ResponseBody
    public String getKeyWord(@PathVariable("keyWord") String keyWord, @PathVariable("radius") Integer radius) {
        double xLng = 37.532878; // 경도
        double yLat = 126.981969; // 위도
        int page = 1; // 페이지 수
        int size = 15; // 한 페이지 내 결과 개수

        JSONObject result = LocalApi.KeyWord.getAll(xLng, yLat, keyWord, radius, page, size);

        return result.toJSONString();
    }

    @GetMapping("/categoryTest/{category}/{radius}")
    @ResponseBody
    public String getCategory(@PathVariable("category") String category, @PathVariable("radius") Integer radius) {
        double xLng = 37.532878; // 경도
        double yLat = 126.981969; // 위도
        int page = 1; // 페이지 수
        int size = 15; // 한 페이지 내 결과 개수

        JSONObject result = LocalApi.Category.getAll(xLng, yLat, category, radius, page, size);

        return result.toJSONString();
    }

    @GetMapping("/data-save-test/{categoryName}")
    @ResponseBody
    public String saveData(@PathVariable("categoryName") String categoryName) {
        Map<String, String> categoryCode = new HashMap<>() {{
            put("문화시설", "CT1"); // 2522
            put("관광명소", "AT4"); // 1111
            put("음식점", "FD6"); // 135651
            put("카페", "CE7"); // 33501
        }};

        placeData.saveAllCategoryData(categoryCode.get(categoryName));
        return "Success";
    }
}
