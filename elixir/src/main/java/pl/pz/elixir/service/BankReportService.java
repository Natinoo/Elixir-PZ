package pl.pz.elixir.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BankReportService {

    public List<String> getBankReport(String bank, List<String> fullReport) {

        List<String> result = new ArrayList<>();

        for (String line : fullReport) {

            if (line.contains(bank)) {
                result.add(line);
            }
        }
        return result;
    }
}