/*
 * Copyright (c) 2016, Leif Lindbäck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package se.leiflindback.oodbook.rentcarWithExAndDesPat.model;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.CarDTO;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.CarRegistry;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.CarRegistryException;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.Printer;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.RegistryCreator;

public class RentalTest {
    ByteArrayOutputStream outContent;
    PrintStream originalSysOut;
    private Map<CarDTO.CarType, Integer> noOfRentedCars;

    @BeforeAll
    public static void setDefaultMatcher() {
        System.setProperty("se.leiflindback.rentcar.matcher.classname",
                           "se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.matching.WildCardMatch");
    }

    @BeforeEach
    public void prepareStreamsAndRentedCounters() {
        noOfRentedCars = new HashMap<>();
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            noOfRentedCars.put(type, 0);
        }

        originalSysOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void cleanUpStreams() {
        outContent = null;
        System.setOut(originalSysOut);
    }

    @Test
    public void testRentCar() {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM, true,
                                      true, "red", false);
        try {
            instance.rentCar(rentedCar);
        } catch (AlreadyBookedException ex) {
            fail("Got Exception.");
            ex.printStackTrace();
        }
        CarDTO expResult = null;
        CarDTO result = carReg.findAvailableCar(rentedCar);
        assertEquals(expResult, result, "Rented car was available.");
        boolean expBookedResult = true;
        boolean bookedResult = carReg.getCarByRegNo(rentedCar).isBooked();
        assertEquals(expBookedResult, bookedResult, "Booked state was not changed.");
    }

    @Test
    public void testRentBookedCar() {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM, true,
                                      true, "red", false);
        try {
            carReg.setBookedStateOfCar(rentedCar, true);
            instance.rentCar(rentedCar);
            fail("Could rent a booked car.");
        } catch (AlreadyBookedException ex) {
            assertTrue(ex.getMessage().contains(rentedCar.getRegNo()),
                       "Wrong exception message, does not contain specified car: " + ex.getMessage());
            assertTrue(ex.getCarThatCanNotBeBooked().getRegNo().equals(rentedCar.getRegNo()),
                       "Wrong car is specified: " + ex.getCarThatCanNotBeBooked());
        }
    }

    @Test
    public void testRentCarThatDoesNotExist() throws AlreadyBookedException {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        CarDTO rentedCar = new CarDTO("wrong", new Amount(1000), CarDTO.CarType.MEDIUM, true,
                                      true, "red", false);
        try {
            instance.rentCar(rentedCar);
            fail("Could rent a non-existing car.");
        } catch (CarRegistryException exc) {
            assertTrue(exc.getMessage().contains(rentedCar.toString()),
                       "Wrong exception message, does not contain specified car: " + exc.
                               getMessage());
        }
    }

    @Test
    public void testPay() {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        Amount price = new Amount(1000);
        CarDTO rentedCar = new CarDTO("abc123", price, CarDTO.CarType.MEDIUM, true,
                                      true, "red", false);
        CashPayment payment = new CashPayment(null);
        try {
            instance.rentCar(rentedCar);
        } catch (AlreadyBookedException ex) {
            fail("Got Exception.");
            ex.printStackTrace();
        }
        instance.pay(payment);
        Amount expResult = price;
        Amount result = instance.getPayment().getTotalCost();
        assertEquals(expResult, result, "Wrong rental cost");
    }

    @Test
    public void testPrintReceipt() {
        Amount price = new Amount(1000);
        String regNo = "abc123";
        CarDTO.CarType size = CarDTO.CarType.MEDIUM;
        boolean AC = true;
        boolean fourWD = true;
        String color = "red";
        boolean booked = true;
        CarDTO rentedCar = new CarDTO(regNo, price, size, AC, fourWD, color, booked);
        Amount paidAmt = new Amount(5000);
        CashPayment payment = new CashPayment(paidAmt);
        Rental instance = new Rental(null, new RegistryCreator().
                                     getCarRegistry());
        try {
            instance.rentCar(rentedCar);
        } catch (AlreadyBookedException ex) {
            fail("Got Exception.");
            ex.printStackTrace();
        }
        instance.pay(payment);
        instance.printReceipt(new Printer());
        LocalDateTime rentalTime = LocalDateTime.now();
        String expResult = "\n\nRented car: " + regNo + "\nCost: " + price
                           + "\nChange: " + paidAmt.minus(price) + "\n\n\n";
        String result = outContent.toString();
        assertTrue(result.contains(expResult), "Wrong printout.");
        assertTrue(result.contains(Integer.toString(rentalTime.getYear())), "Wrong rental year.");
        assertTrue(result.contains(Integer.toString(rentalTime.getMonthValue())),
                   "Wrong rental month.");
        assertTrue(result.contains(Integer.toString(rentalTime.getDayOfMonth())),
                   "Wrong rental day.");
        assertTrue(result.contains(Integer.toString(rentalTime.getHour())), "Wrong rental hour.");
        assertTrue(result.contains(Integer.toString(rentalTime.getMinute())), "Wrong rental minute.");
    }

    @Test
    public void testAddSingleObservers() {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        int noOfObservers = 3;
        for (int i = 0; i < noOfObservers; i++) {
            instance.addRentalObserver(new CountingObserver());
        }
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM, true,
                                      true, "red", false);
        CashPayment payment = new CashPayment(null);
        try {
            instance.rentCar(rentedCar);
        } catch (AlreadyBookedException ex) {
            fail("Got Exception.");
            ex.printStackTrace();
        }
        instance.pay(payment);
        Map<CarDTO.CarType, Integer> expResult = new HashMap<>();
        expResult.put(CarDTO.CarType.SMALL, 0);
        expResult.put(CarDTO.CarType.MEDIUM, noOfObservers);
        expResult.put(CarDTO.CarType.LARGE, 0);
        Map<CarDTO.CarType, Integer> result = noOfRentedCars;
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            assertEquals(expResult.get(type), result.get(type),
                         "Observers were not correctly notified.");
        }
    }

    @Test
    public void testAddMultipleObservers() {
        CarRegistry carReg = new RegistryCreator().getCarRegistry();
        Rental instance = new Rental(null, carReg);
        int noOfObservers = 3;
        List<RentalObserver> observers = new ArrayList<>();
        for (int i = 0; i < noOfObservers; i++) {
            observers.add(new CountingObserver());
        }
        instance.addRentalObservers(observers);
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.LARGE, true,
                                      true, "red", false);
        CashPayment payment = new CashPayment(null);
        try {
            instance.rentCar(rentedCar);
        } catch (AlreadyBookedException ex) {
            fail("Got Exception.");
            ex.printStackTrace();
        }
        instance.pay(payment);
        Map<CarDTO.CarType, Integer> expResult = new HashMap<>();
        expResult.put(CarDTO.CarType.SMALL, 0);
        expResult.put(CarDTO.CarType.MEDIUM, 0);
        expResult.put(CarDTO.CarType.LARGE, noOfObservers);
        Map<CarDTO.CarType, Integer> result = noOfRentedCars;
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            assertEquals(expResult.get(type), result.get(type),
                         "Observers were not correctly notified.");
        }
    }

    private class CountingObserver implements RentalObserver {
        @Override
        public void newRental(CarDTO rentedCar) {
            int noOfRentedCarsOfThisType = noOfRentedCars.get(rentedCar.getSize()) + 1;
            noOfRentedCars.put(rentedCar.getSize(), noOfRentedCarsOfThisType);
        }
    }
}
