package example.service;

import example.entity.Combo;
import example.repository.ComboRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComboServiceTest {

    @Mock
    private ComboRepository comboRepository;

    @InjectMocks
    private ComboService comboService;

    @Test
    @DisplayName("Lấy danh sách tất cả Combo thành công")
    void testGetAll() {
        when(comboRepository.findAll()).thenReturn(Arrays.asList(new Combo(), new Combo()));

        List<Combo> result = comboService.getAll();

        assertEquals(2, result.size());
        verify(comboRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Lưu mới Combo với mặc định active = true khi chưa gán")
    void testSave_NewCombo_DefaultActive() {
        Combo combo = new Combo();
        // ID is null
        
        comboService.save(combo);

        assertTrue(combo.getActive());
        verify(comboRepository, never()).findById(any());
        verify(comboRepository, times(1)).save(combo);
    }

    @Test
    @DisplayName("Cập nhật Combo nhưng giữ nguyên Active cũ nếu giá trị mới là null")
    void testSave_UpdateCombo_KeepActive() {
        Combo combo = new Combo();
        combo.setId(1);
        combo.setActive(null);

        Combo existingCombo = new Combo();
        existingCombo.setId(1);
        existingCombo.setActive(false);

        when(comboRepository.findById(1)).thenReturn(existingCombo);

        comboService.save(combo);

        assertFalse(combo.getActive()); // Giữ nguyên active cũ = false
        verify(comboRepository).findById(1);
        verify(comboRepository).save(combo);
    }

    @Test
    @DisplayName("Cập nhật Combo với phiên bản mới đã có Active value")
    void testSave_UpdateCombo_WithActiveProvided() {
        Combo combo = new Combo();
        combo.setId(1);
        combo.setActive(true);

        // Do không gán null vào active, hệ thống không lấy dòng cũ
        comboService.save(combo);

        assertTrue(combo.getActive());
        verify(comboRepository).save(combo);
    }

    @Test
    @DisplayName("Xóa Combo theo ID thành công")
    void testDelete() {
        comboService.delete(1);
        verify(comboRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Cập nhật lại trạng thái Active thông qua Switch")
    void testUpdateStatus_Success() {
        Combo existingCombo = new Combo();
        existingCombo.setId(1);
        existingCombo.setActive(false);

        when(comboRepository.findById(1)).thenReturn(existingCombo);

        comboService.updateStatus(1, true);

        assertTrue(existingCombo.getActive());
        verify(comboRepository).save(existingCombo);
    }

    @Test
    @DisplayName("Không cập nhật trạng thái nếu không tìm thấy Combo tương ứng")
    void testUpdateStatus_NotFound() {
        when(comboRepository.findById(99)).thenReturn(null);

        comboService.updateStatus(99, true);

        verify(comboRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tìm kiếm Combo thành công và nhận List kết quả")
    void testSearch() {
        when(comboRepository.search(anyString(), anyString(), anyDouble(), anyBoolean()))
                .thenReturn(Arrays.asList(new Combo()));

        List<Combo> result = comboService.search("Bắp", "Ngọt", 100.0, true);

        assertEquals(1, result.size());
        verify(comboRepository).search("Bắp", "Ngọt", 100.0, true);
    }

    @Test
    @DisplayName("Tìm Combo theo ID lấy thành công Object")
    void testFindById() {
        Combo combo = new Combo();
        combo.setId(10);
        when(comboRepository.findById(10)).thenReturn(combo);

        Combo result = comboService.findById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }
}
