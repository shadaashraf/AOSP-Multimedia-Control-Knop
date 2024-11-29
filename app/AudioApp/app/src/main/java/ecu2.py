import socket
import struct
import time
import threading
import random

# Simulate the PZEM module
class PZEMSimulator:
    def voltage(self):
        return random.uniform(210, 230)  # Simulated voltage in volts

    def current(self):
        return random.uniform(0, 10)  # Simulated current in amps

    def power(self):
        return random.uniform(100, 2000)  # Simulated power in watts

    def energy(self):
        return random.uniform(0, 10000)  # Simulated energy in kWh

# TCP server settings
HOST = "192.168.1.81"  # Replace with your PC's IP
PORT = 5000            # Port number

# Instantiate the PZEM simulator
pzem = PZEMSimulator()

def handle_client(client_socket):
    try:
        while True:
            command = client_socket.recv(1)  # Receive 1-byte command
            if not command:
                break

            command = command[0]
            value = None

            # Handle commands
            if command == 1:  # Voltage
                value = pzem.voltage()
            elif command == 2:  # Current
                value = pzem.current()
            elif command == 3:  # Power
                value = pzem.power()
            elif command == 4:  # Energy
                value = pzem.energy()
            else:
                client_socket.sendall(b'\xFF')  # Error response
                continue

            if value is not None:
                # Pack the float as 4 bytes in little-endian format
                packed_value = struct.pack('<f', value)
                client_socket.sendall(packed_value)
            else:
                client_socket.sendall(b'\xFF')  # Error response
    finally:
        client_socket.close()

def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((HOST, PORT))
    server_socket.listen(5)  # Allow up to 5 queued connections

    print(f"Server started on {HOST}:{PORT}")
    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connection from {addr}")
        client_handler = threading.Thread(target=handle_client, args=(client_socket,))
        client_handler.start()

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\nServer shutting down.")
