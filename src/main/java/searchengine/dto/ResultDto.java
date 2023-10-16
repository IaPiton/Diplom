package searchengine.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;


import java.util.List;

@Getter
@Setter
public class ResultDto {

    private boolean result;

    private String error;

    private int count;

    private HttpStatus status;

    private List<SearchDto> data;

    public ResultDto(boolean result) {
        this.result = result;
    }

    public ResultDto(boolean result, HttpStatus status) {
        this.result = result;
        this.status = status;
    }

    public ResultDto(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public ResultDto(boolean result, String error, HttpStatus status) {
        this.result = result;
        this.error = error;
        this.status = status;
    }

    public ResultDto(boolean result, int count, List<SearchDto> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public ResultDto(boolean result, int count, List<SearchDto> data, HttpStatus status) {
        this.result = result;
        this.count = count;
        this.data = data;
        this.status = status;
    }
}