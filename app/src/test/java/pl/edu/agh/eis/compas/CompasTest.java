package pl.edu.agh.eis.compas;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompasTest {

    private final Compas compas = new Compas();
    private static final double DELTA = 1e-15;

    @Test
    public void testOdejmij() {
        assertEquals(-10.0, compas.odejmij(10, 20), DELTA);
        assertEquals(10.0, compas.odejmij(20, 10), DELTA);
        assertEquals(20.0, compas.odejmij(10, 350), DELTA);
        assertEquals(-20.0, compas.odejmij(350, 10), DELTA);
        assertEquals(-180.0, compas.odejmij(180, 0), DELTA);
        assertEquals(-179.0, compas.odejmij(0, 179), DELTA);
        assertEquals(179.0, compas.odejmij(179, 0), DELTA);
    }

    @Test
    public void testPoprawZakres() {
        assertEquals(0.0, compas.poprawZakres(360), DELTA);
        assertEquals(40.0, compas.poprawZakres(400), DELTA);
        assertEquals(350.0, compas.poprawZakres(-10), DELTA);
        assertEquals(180.0, compas.poprawZakres(180), DELTA);
        assertEquals(0.0, compas.poprawZakres(0), DELTA);
    }
}