package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"
)

type Hooks struct {
	OnCommand func(command string)
	OnOutput  func(line string)
	OnStart   func(pid int)
	OnStop    func(exitCode int)
}

func RunWithHooks(command string, hooks Hooks) error {
	cmdParts := strings.Fields(command)
	if len(cmdParts) == 0 {
		return fmt.Errorf("empty command")
	}

	cmd := exec.Command(cmdParts[0], cmdParts[1:]...)

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return err
	}

	stdin, err := cmd.StdinPipe()
	if err != nil {
		return err
	}

	cmd.Stderr = cmd.Stdout
	cmd.Env = os.Environ()

	go func() {
		scanner := bufio.NewScanner(stdout)
		for scanner.Scan() {
			line := scanner.Text()
			if hooks.OnOutput != nil {
				hooks.OnOutput(line)
			}

			fmt.Println(line)
		}
	}()

	go func() {
		scanner := bufio.NewScanner(os.Stdin)
		for scanner.Scan() {
			command := scanner.Text()
			if hooks.OnCommand != nil {
				hooks.OnCommand(command)
			}
			stdin.Write([]byte(command + "\n"))
		}
	}()

	if err := cmd.Start(); err != nil {
		return err
	}

	if hooks.OnStart != nil {
		hooks.OnStart(cmd.Process.Pid)
	}

	if err := cmd.Wait(); err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			if hooks.OnStop != nil {
				hooks.OnStop(exitErr.ExitCode())
			}
		}
		return err
	}

	if hooks.OnStop != nil {
		hooks.OnStop(0)
	}

	return nil
}

func main() {
	var command string
	flag.StringVar(&command, "command", "", "The command to execute")
	flag.Parse()

	if command == "" {
		log.Fatal("No command specified")
	}

	hooks := Hooks{
		OnCommand: func(command string) {
			log.Printf("Command sent: %s", command)
		},
		OnOutput: func(line string) {
		},
		OnStart: func(pid int) {
			log.Printf("Process started with PID: %d", pid)
		},
		OnStop: func(exitCode int) {
			log.Printf("Process stopped with exit code: %d", exitCode)
		},
	}

	if err := RunWithHooks(command, hooks); err != nil {
		log.Printf("Process error: %v", err)
	}
}
