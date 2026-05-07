package rs.nopressure.wear.webshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.nopressure.wear.webshop.dto.category.CategoryRequest;
import rs.nopressure.wear.webshop.dto.category.CategoryResponse;
import rs.nopressure.wear.webshop.exception.DuplicateResourceException;
import rs.nopressure.wear.webshop.exception.ResourceNotFoundException;
import rs.nopressure.wear.webshop.model.Category;
import rs.nopressure.wear.webshop.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .build();

        request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Electronic devices");
    }

    @Test
    void create_ShouldReturnCategoryResponse_WhenValidRequest() {
        when(categoryRepository.existsByName(anyString())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void create_ShouldThrowDuplicateException_WhenNameExists() {
        when(categoryRepository.existsByName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnCategory_WhenExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Electronics");
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotExists() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getAll_ShouldReturnAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.getAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    void delete_ShouldDeleteCategory_WhenExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void delete_ShouldThrowNotFoundException_WhenNotExists() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}