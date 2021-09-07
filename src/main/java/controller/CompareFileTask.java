package controller;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

import model.*;

public class CompareFileTask implements Runnable
{
    ComparisonPair pair;
    ComparisonTable table;

    public CompareFileTask(ComparisonPair inPair, ComparisonTable inTable)
    {
        pair = inPair;
        table = inTable;
    }

    @Override
    public void run()
    {
        //Compare file contents of the 2 files
        try {
            Path file1 = pair.getFirstPath();
            String file1Contents = new String(Files.readAllBytes(file1), StandardCharsets.UTF_8);
            Path file2 = pair.getSecondPath();
            String file2Contents = new String(Files.readAllBytes(file2), StandardCharsets.UTF_8);
            int[][] subsolutions = new int[file1Contents.length()][file1Contents.length()];
            boolean[][] directionLeft = new boolean[file1Contents.length()][file1Contents.length()];    
            int matches = 0;
            int columnNumber;
            int rowNumber;
            double similarity;

                
            Arrays.fill(subsolutions[0], 0);

            for (int ii = 0; ii < subsolutions.length; ii++)
            {
                subsolutions[ii][0] = 0;
            }

            //LCS Algorithm from assignment specification, with slight modifications
            for(int ii = 1; ii < file1Contents.length(); ii++)
            {
                for (int jj = 1; jj < file2Contents.length(); jj++)
                {
                    //System.out.println(pair.getFirstPath().getFileName() + " VS " + pair.getSecondPath().getFileName());


                    if (file1Contents.charAt(ii - 1) == file2Contents.charAt(jj - 1))
                    {
                        //Get previous best result
                        subsolutions[ii][jj] = subsolutions[ii - 1][jj - 1] + 1;
                    }
                    else if (subsolutions[ii - 1][jj] > subsolutions[ii][jj - 1])
                    {
                        subsolutions[ii][jj] = subsolutions[ii -1][jj];
                        directionLeft[ii][jj] = true;
                    }
                    else
                    {
                        subsolutions[ii][jj] = subsolutions[ii][jj - 1];
                        directionLeft[ii][jj] = false;

                    }

                }
            }

            columnNumber = file1Contents.length() - 1;
            rowNumber = file2Contents.length() - 1;

            while (columnNumber > 0 && rowNumber > 0)
            {
                if (file1Contents.charAt(columnNumber) == file2Contents.charAt(rowNumber))
                {
                    matches += 1;
                    rowNumber -= 1;
                    columnNumber -= 1;
                }
                else if (directionLeft[rowNumber][columnNumber])
                {
                    rowNumber -= 1;
                }
                else
                {
                    columnNumber -= 1;

                }


            }

            System.out.println("matches: " + matches);

            similarity = (double)(((double)(matches * 2)) / ((double)(file1Contents.length() + file2Contents.length() - 2)));


            //Construct new ComparisonResult object with file names and the calculated similarity and add to table
            table.queueResult(new ComparisonResult(file1.toFile().getName(), file2.toFile().getName(), similarity));



        } catch (IOException e) {
            //TODO: handle exception
            System.out.println("Error");

        }


    }
    
    
}
