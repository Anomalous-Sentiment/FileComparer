package model;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import controller.*;
import javafx.application.Platform;
import view.*;

public class ComparisonTable 
{
    private List<CompareFileTask> compareTasks;
    private List<Path> fileList;
    private LinkedBlockingQueue<ComparisonResult> resultQueue;
    private List<ComparisonResult> finished;
    private List<ComparisonResult> highSimilarity;
    private FileIO fileHandler;
    private File directory;
    public CompareFileTask POISON = new CompareFileTask(null, null);
    private ComparerUI ui;
    private ExecutorService executor;
    private double progress;

    public ComparisonTable(ComparerUI inUi, File inDirectory)
    {
        directory = inDirectory;
        ui = inUi;
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        compareTasks = new ArrayList<>();
        finished = new ArrayList<>();
        resultQueue = new LinkedBlockingQueue<>();
        fileHandler = new FileIO();
        fileList = new ArrayList<>();
        highSimilarity = new ArrayList<>();
        progress = 0.0;
    }

    public void start()
    {
        Path[] fileArray;
        ComparisonPair tempPair;
        Iterator<CompareFileTask> iter;
        ComparisonResult newResult;

        //Get paths to all files
        try {
            Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    fileList.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });

            //Loop over list of files to get all combinations (tasks) there *should* be no duplicate tasks
            fileArray = fileList.toArray(new Path[1]);

            for (int ii = 0; ii < fileArray.length - 1; ii++)
            {
                for (int jj = ii + 1; jj < fileArray.length; jj++)
                {
                    tempPair = new ComparisonPair(fileArray[ii], fileArray[jj]);
                    compareTasks.add(new CompareFileTask(tempPair, this));

                }
            }

            iter = compareTasks.iterator();

            //Submit every task to the executor service
            while (iter.hasNext())
            {
                executor.submit(iter.next());
            }


            //Can shutdown executor so it doesn't accept any new tasks after finished those already
            //submitted
            executor.shutdown();

            //Loop until all tasks are complete (interrupted exception thrown)
            while (true)
            {
                //Take new result (Can throw interrupted exception)
                newResult = resultQueue.take();


                System.out.println(newResult.getFile1() + "->" + newResult.getFile2() + "similarity: " + newResult.getSimilarity());

                
                //Add to results.csv

                //Add to finished
                finished.add(newResult);

                //Update progress
                progress = finished.size() / compareTasks.size();

                //Check for high similarity
                if (newResult.getSimilarity() > 0.5)
                {
                    highSimilarity.add(newResult);
                }

                updateUI();

                //If all tasks complete, interrupt thread
                if (finished.size() == compareTasks.size())
                {
                    System.out.println("test");

                    Thread.currentThread().interrupt();
                }


            }

        } catch (IOException e) {
            //TODO: handle exception
        }
        catch (InterruptedException interuptException)
        {
            //Exit loop. All files finished comparing
        }

        System.out.println("Comparisons: " + compareTasks.size());


        
    }

    public void queueResult(ComparisonResult newResult)
    {
        resultQueue.add(newResult);
    }

    public void updateUI()
    {
        Platform.runLater(new Runnable(){
            @Override
            public void run()
            {
                ui.setResults(highSimilarity);
                ui.setProgress(progress);
            }
        });
    }

}
