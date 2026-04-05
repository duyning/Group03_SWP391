package example.repository;

import example.entity.Combo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
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
class ComboRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Combo> query;

    @InjectMocks
    private ComboRepository comboRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Lấy danh sách tất cả Combo thành công")
    void testFindAll() {
        when(session.createQuery("FROM Combo", Combo.class)).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new Combo(), new Combo()));

        List<Combo> result = comboRepository.findAll();

        assertEquals(2, result.size());
        verify(session).createQuery("FROM Combo", Combo.class);
    }

    @Test
    @DisplayName("Tìm Combo theo ID thành công")
    void testFindById() {
        Combo combo = new Combo();
        combo.setId(1);
        when(session.get(Combo.class, 1)).thenReturn(combo);

        Combo result = comboRepository.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("Thêm mới Combo thành công (dùng persist)")
    void testSave_New() {
        Combo combo = new Combo();
        combo.setId(null);

        comboRepository.save(combo);

        verify(session, times(1)).persist(combo);
        verify(session, never()).merge(any());
    }

    @Test
    @DisplayName("Cập nhật Combo thành công (dùng merge)")
    void testSave_Update() {
        Combo combo = new Combo();
        combo.setId(1);

        comboRepository.save(combo);

        verify(session, times(1)).merge(combo);
        verify(session, never()).persist(any());
    }

    @Test
    @DisplayName("Xóa Combo theo ID thành công")
    void testDeleteById_Success() {
        Combo combo = new Combo();
        combo.setId(1);
        when(session.get(Combo.class, 1)).thenReturn(combo);

        comboRepository.deleteById(1);

        verify(session, times(1)).remove(combo);
    }

    @Test
    @DisplayName("Xóa Combo theo ID không thực hiện khi Combo không tồn tại")
    void testDeleteById_NotFound() {
        when(session.get(Combo.class, 99)).thenReturn(null);

        comboRepository.deleteById(99);

        verify(session, never()).remove(any());
    }

    @Test
    @DisplayName("Tìm kiếm Combo với tất cả các tham số (name, description, maxPrice, status)")
    void testSearch_WithAllParameters() {
        when(session.createQuery(anyString(), eq(Combo.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new Combo()));

        // Vì Query setParameter trả về Query impl, Mockito trên generic interface
        // cần cấu hình return chính query mockup
        when(query.setParameter(anyString(), any())).thenReturn(query);

        List<Combo> result = comboRepository.search("Bắp", "Ngọt", 100.0, true);

        assertEquals(1, result.size());
        verify(session).createQuery(contains("AND lower(c.comboName) LIKE :name"), eq(Combo.class));
        verify(query).setParameter("name", "%bắp%");
        verify(query).setParameter("description", "%ngọt%");
        verify(query).setParameter("maxPrice", 100.0);
        verify(query).setParameter("status", true);
    }

    @Test
    @DisplayName("Tìm kiếm Combo mà không có tiêu chí nào (hỗ trợ get list cơ bản)")
    void testSearch_WithoutParameters() {
        when(session.createQuery(anyString(), eq(Combo.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new Combo(), new Combo()));

        List<Combo> result = comboRepository.search(null, "", null, null);

        assertEquals(2, result.size());
        verify(session).createQuery(contains("FROM Combo c WHERE 1=1"), eq(Combo.class));
        verify(query, never()).setParameter(anyString(), any());
    }
}
